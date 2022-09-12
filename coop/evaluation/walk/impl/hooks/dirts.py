import glob
import os
import re

from datetime import datetime
from pathlib import Path
from time import time
from typing import Optional

from ...base import Hook
from .....db.base import DBConnection
from .....db.testing import DBTestReport
from .....models.scm.base import Commit, Repository
from .....util.logging.logger import get_logger
from .....models.scm.impl.git import GitClient
from .....models.testing.base import TestReport, DirtsDurationReport
from .....util.os.exec import check_executable_exists, SubprocessContainer

_LOGGER = get_logger(__name__)


class DIRTSMavenHook(Hook):

    def __init__(
            self,
            repository: Repository,
            git_client: GitClient,
            type: str,
            output_path: Optional[str] = None,
            java_version: str = "",
            cmd: str = "edu.tum.sse.dirts:dirts-maven-plugin:0.1-SNAPSHOT:",
            clean: bool = True,
            options=None,
            report_name: Optional[str] = None,
            connection: Optional[DBConnection] = None,
            java11_special: bool = False
    ) -> None:
        super().__init__(repository, output_path, git_client, java11_special)
        if options is None:
            options = ["-Dmaven.test.failure.ignore=true",
                       "-e"]
        if self.output_path:
            self.cache_dir = os.path.join(self.output_path, ".dirts-hook")
        else:
            self.cache_dir = os.path.join(self.repository.path, ".dirts-hook")
        self.java_version = java_version
        self.executable: str = "mvn"
        if not check_executable_exists(self.executable):
            raise Exception("Executable {} does not exist".format(self.executable))
        self.cmd = cmd
        self.type = type
        self.clean = clean
        self.options = options
        self.report_name = report_name
        self.connection = connection


    def _run_dirts(self, commit: Commit) -> bool:
        """
        Run DIRTS tool.

        :return:
        """
        # keep track of current working directory
        tmp_path = os.getcwd()

        os.makedirs(self.cache_dir, exist_ok=True)  # recursively create dirs

        # navigate into repo
        os.chdir(self.repository.path)

        has_failed = False

        # checkout parent commit
        parent_commit = self.git_client.get_parent_commit(commit_sha=commit.commit_str)
        self.git_client.git_repo.git.checkout(parent_commit, force=True)
        self.git_client.git_repo.git.reset(parent_commit, hard=True)

        has_failed |= self._update_pom()

        if self.clean:
            command: str = "{0}{1} clean {2}clean".format(
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

        # run dirts to generate temporary files
        command: str = "{0}{1} test-compile {2}{3} {4}".format(
            (self.java_version + " " if self.java_version else ""),
            self.executable,
            self.cmd,
            self.type,
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
        has_failed |= proc.exit_code != 0

        # checkout actual commit
        self.git_client.git_repo.git.checkout(commit.commit_str, force=True)
        self.git_client.git_repo.git.reset(commit.commit_str, hard=True)

        has_failed |= self._update_pom()

        # prepare command, no clean here
        command: str = "{0}{1} test-compile {2}{3} surefire:test {4}".format(
            (self.java_version + " " if self.java_version else ""),
            self.executable,
            self.cmd,
            self.type,
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
        failures = re.search("(The forked VM terminated without properly saying goodbye. VM crash or System.exit called?)", proc.output)
        has_failed |= failures is not None and len(failures.groups()) != 0

        # parse durations of knowledge sources
        output = proc.output

        durations = []
        durations_patterns = re.findall("(dirts-maven-plugin(.|\\n)*?DIRTS excluded:)", output)
        for durations_pattern, _ in durations_patterns:
            duration_project_importer_matcher = re.search(
                "TIME.* Importing graph of the old revision took (.*) seconds",
                durations_pattern)
            duration_typesolver_initializer_matcher = re.search("TIME.* Setting up type solvers took (.*) seconds",
                                                                durations_pattern)
            duration_parser_matcher = re.search("TIME.* Parsing took (.*) seconds", durations_pattern)
            duration_type_finder_matcher = re.search("TIME.* Finding tests took (.*) seconds", durations_pattern)
            duration_change_analyzer_matcher = re.search("TIME.* Calculating changes took (.*) seconds",
                                                         durations_pattern)
            duration_graph_cropper_matcher = re.search("TIME.* Cropping dependency graph took (.*) seconds",
                                                       durations_pattern)
            duration_dependency_analyzer_matcher = re.search("TIME.* Calculating dependencies took (.*) seconds",
                                                             durations_pattern)
            duration_graph_combiner_matcher = re.search("TIME.* Combining graphs took (.*) seconds",
                                                        durations_pattern)
            duration_project_exporter_matcher = re.search(
                "TIME.* Exporting graph of the new revision took (.*) seconds",
                durations_pattern)

            duration_all_matcher = re.search("TIME.* Selecting tests altogether took (.*) seconds",
                                             durations_pattern)

            if (duration_project_importer_matcher is not None and duration_typesolver_initializer_matcher is not None
                    and duration_parser_matcher is not None and duration_type_finder_matcher is not None and
                    duration_change_analyzer_matcher is not None and duration_dependency_analyzer_matcher is not None and
                    duration_graph_cropper_matcher is not None):
                durations_report = DirtsDurationReport(
                    duration_project_importer=float(duration_project_importer_matcher.group(1)),
                    duration_typesolver_initializer=float(duration_typesolver_initializer_matcher.group(1)),
                    duration_parser=float(duration_parser_matcher.group(1)),
                    duration_test_finder=float(duration_type_finder_matcher.group(1)),
                    duration_change_analyzer=float(duration_change_analyzer_matcher.group(1)),
                    duration_graph_cropper=float(duration_graph_cropper_matcher.group(1)),
                    duration_dependency_analyzer=float(duration_dependency_analyzer_matcher.group(1)),
                    duration_graph_combiner=float(duration_graph_combiner_matcher.group(1)),
                    duration_project_exporter=float(duration_project_exporter_matcher.group(1)),
                    duration_all=float(duration_all_matcher.group(1)),
                )
                durations.append(durations_report)

        if self.connection and self.report_name:
            try:
                with self.connection.create_session_ctx() as session:
                    report = TestReport(
                        name=self.report_name,
                        duration=proc.end_to_end_time,
                        timestamp=datetime.now(),
                        commit_str=commit.commit_str,
                        commit=commit,
                        log=output.replace("\x00", ""),
                        has_failed=has_failed,
                        durations_dirts=durations
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
        return self._run_dirts(commit)


    def getExcludesFilePath(self, pom_path):
        return os.path.join(Path(pom_path).parent, "DIRTSExcludes")
