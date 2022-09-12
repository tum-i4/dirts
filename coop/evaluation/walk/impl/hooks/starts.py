"""
This module contains hooks for using the tool STARTS in repository walkers.
https://github.com/TestingResearchIllinois/starts
"""
# Copyright (c) 2019-present. The coop authors.
import glob
import os
import re
import xml.etree.ElementTree as ET
from datetime import datetime
from time import time
from typing import Optional

from ...base import Hook
from .....db.base import DBConnection
from .....db.testing import DBTestReport
from .....models.scm.base import Commit, Repository
from .....util.logging.logger import get_logger
from .....models.scm.impl.git import GitClient
from .....models.testing.base import TestReport, StartsDurationReport
from .....util.os.exec import check_executable_exists, SubprocessContainer

_LOGGER = get_logger(__name__)


class STARTSMavenHook(Hook):
    def __init__(
            self,
            repository: Repository,
            git_client: GitClient,
            output_path: Optional[str] = None,
            java_version: str = "",
            cmd: str = "edu.illinois:starts-maven-plugin:1.4-SNAPSHOT:",  # built manually
            clean: bool = True,
            options=None,
            report_name: Optional[str] = None,
            connection: Optional[DBConnection] = None,
    ) -> None:
        super().__init__(repository, output_path, git_client)
        if options is None:
            options = ["-DstartsLogging=FINEST",
                       "-Dmaven.test.failure.ignore=true",
                       "-e"]
        if self.output_path:
            self.cache_dir = os.path.join(self.output_path, ".starts-hook")
        else:
            self.cache_dir = os.path.join(self.repository.path, ".starts-hook")
        self.java_version = java_version
        self.executable: str = "mvn"
        if not check_executable_exists(self.executable):
            raise Exception("Executable {} does not exist".format(self.executable))
        self.cmd = cmd
        self.clean = clean
        self.options = options
        self.report_name = report_name
        self.connection = connection

    def _run_starts(self, commit: Commit) -> bool:
        """
        Run STARTS tool.

        :return:
        """
        # checkout and reset hard to actual commit
        self.git_client.git_repo.git.checkout(commit.commit_str, force=True)
        self.git_client.git_repo.git.reset(commit.commit_str, hard=True)

        os.makedirs(self.cache_dir, exist_ok=True)  # recursively create dirs

        # keep track of current working directory
        tmp_path = os.getcwd()

        # navigate into repo
        os.chdir(self.repository.path)

        has_failed = False

        # checkout parent commit
        parent_commit = self.git_client.get_parent_commit(commit_sha=commit.commit_str)
        self.git_client.git_repo.git.checkout(parent_commit, force=True)

        has_failed |= self._update_pom()

        if self.clean:
            command: str = "{0}{1} clean {2}:clean".format(
                (self.java_version + " " if self.java_version else ""),
                self.executable,
                self.cmd
            )

            # prepare cache dir/file
            cache_file = "run_{}.log".format(
                int(time() * 1000)
            )  # run identified by timestamp
            cache_file_path = os.path.join(self.cache_dir, cache_file)

            proc: SubprocessContainer = SubprocessContainer(
                command=command, output_filepath=cache_file_path
            )
            proc.execute(capture_output=True, shell=True, timeout=1000.0)

        # run starts:run to generate temporary files
        command: str = "{0}{1} test-compile {2}run {3}".format(
            (self.java_version + " " if self.java_version else ""),
            self.executable,
            self.cmd,
            " ".join(self.options),
        )

        # prepare cache dir/file
        cache_file = "run_{}.log".format(
            int(time() * 1000)
        )  # run identified by timestamp
        cache_file_path = os.path.join(self.cache_dir, cache_file)

        proc: SubprocessContainer = SubprocessContainer(
            command=command, output_filepath=cache_file_path
        )
        proc.execute(capture_output=True, shell=True, timeout=1000.0)
        has_failed |= (proc.exit_code != 0)

        # checkout actual commit
        self.git_client.git_repo.git.checkout(commit.commit_str, force=True)

        has_failed |= self._update_pom()

        # prepare command, no clean here
        command: str = "{0}{1} test-compile {2}run surefire:test {3}".format(
            (self.java_version + " " if self.java_version else ""),
            self.executable,
            self.cmd,
            " ".join(self.options),
        )

        # prepare cache dir/file
        cache_file = "run_{}.log".format(
            int(time() * 1000)
        )  # run identified by timestamp
        cache_file_path = os.path.join(self.cache_dir, cache_file)

        proc: SubprocessContainer = SubprocessContainer(
            command=command, output_filepath=cache_file_path
        )
        proc.execute(capture_output=True, shell=True, timeout=1000.0)
        has_failed |= (proc.exit_code != 0)

        # handle weird case in rocketmq-dashboard
        failures = re.search(
            "(The forked VM terminated without properly saying goodbye. VM crash or System.exit called?)", proc.output)
        has_failed |= failures is not None and len(failures.groups()) != 0

        # parse durations of knowledge sources
        output = proc.output

        durations = []
        durations_patterns = re.findall("(starts-maven-plugin:.*:run(.|\\n)*?RUN-MOJO-TOTAL.*)", output)
        for durations_pattern, _ in durations_patterns:
            duration_jdeps_matcher = re.search(
                "PROFILE.* createLoadable\(runJDeps\): (.*)"
                , durations_pattern
            )
            duration_graph_matcher = re.search(
                "PROFILE.* createLoadable\(buildGraph\): (.*)"
                , durations_pattern
            )
            duration_all_matcher = re.search(
                "PROFILE.* RUN-MOJO-TOTAL: (.*)"
                , durations_pattern
            )

            if (duration_jdeps_matcher is not None and duration_graph_matcher is not None
                    and duration_all_matcher is not None):
                durations_report = StartsDurationReport(
                    duration_jdeps=float(duration_jdeps_matcher.group(1).replace(".", "").replace(",", ".")),
                    duration_graph=float(duration_graph_matcher.group(1).replace(".", "").replace(",", ".")),
                    duration_all=float(duration_all_matcher.group(1).replace(".", "").replace(",", "."))
                )
                durations.append(durations_report)

        if self.connection and self.report_name:
            try:
                with self.connection.create_session_ctx() as session:
                    report = TestReport(
                        name=self.report_name,
                        duration=proc.end_to_end_time,
                        timestamp=datetime.now(),
                        commit=commit,
                        commit_str=commit.commit_str,
                        log=output.replace("\x00", ""),
                        has_failed=has_failed,
                        durations_starts=durations
                    )
                    DBTestReport.create_or_update(report=report, session=session)
                    session.commit()
            except Exception as e:
                _LOGGER.debug(e)
                _LOGGER.debug(
                    "Failed to store {} results into DB.".format(self.__class__)
                )

        self.cleanExcludesFiles()

        # return to previous directory
        os.chdir(tmp_path)

        return not has_failed

    def run(self, commit: Commit) -> bool:
        return self._run_starts(commit)
