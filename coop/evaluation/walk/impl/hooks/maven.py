# Copyright (c) 2019-present. The coop authors.
import glob
import os
import re
from datetime import datetime
from time import time
from typing import Optional, List
import xml.etree.ElementTree as ET

from ...base import Hook
from .....db.base import DBConnection
from .....db.testing import DBTestReport
from .....models.scm.base import Commit, Repository
from .....util.logging.logger import get_logger
from .....db.scm import DBCommit
from .....models.testing.base import TestReport
from .....util.os.exec import check_executable_exists, SubprocessContainer

_LOGGER = get_logger(__name__)


class MavenHook(Hook):
    """
    Simple hook to execute `mvn $cmd`, e.g., `mvn test` for retest-all test execution.
    """

    def __init__(
            self,
            repository: Repository,
            git_client,
            output_path: Optional[str] = None,
            java_version: str = "",
            cmd: str = "test-compile surefire:test",
            clean: bool = True,
            options: Optional[List[str]] = None,
            report_name: Optional[str] = None,
            connection: Optional[DBConnection] = None,
    ) -> None:
        super().__init__(repository, output_path)
        if options is None:
            options = [
                "-DfailIfNoTests=false",
                "-Dmaven.test.failure.ignore=true",
                "-Dtest='**/*Tests.java,**/*Test.java,**/Test*.java,**/*TestCase.java,**/*TestCases.java'",
            ]
        self.git_client = git_client
        if self.output_path:
            self.cache_dir = os.path.join(self.output_path, ".maven-hook")
        else:
            self.cache_dir = os.path.join(self.repository.path, ".maven-hook")
        self.java_version = java_version
        self.executable: str = "mvn"
        if not check_executable_exists(self.executable):
            raise Exception("Executable {} does not exist".format(self.executable))
        self.cmd = cmd
        self.clean = clean
        self.options = options
        self.report_name = report_name
        self.connection = connection

    def run(self, commit: Commit) -> bool:

        # checkout and reset hard to actual commit
        self.git_client.git_repo.git.checkout(commit.commit_str, force=True)
        self.git_client.git_repo.git.reset(commit.commit_str, hard=True)

        # keep track of current working directory
        tmp_path: str = os.getcwd()

        os.makedirs(self.cache_dir, exist_ok=True)

        has_failed = False

        # navigate into repo
        os.chdir(self.repository.path)

        has_failed |= self._update_pom()

        if self.clean:
            command: str = "{0}{1} clean".format(
                (self.java_version + " " if self.java_version else ""), self.executable
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

        # prepare command
        command: str = "{0}{1} {2} {3}".format(
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
        has_failed |= proc.exit_code != 0

        # handle weird case in rocketmq-dashboard
        failures = re.search("(The forked VM terminated without properly saying goodbye. VM crash or System.exit called?)", proc.output)
        has_failed |= failures is not None and len(failures.groups()) != 0

        if self.connection and self.report_name:
            try:
                with self.connection.create_session_ctx() as session:
                    report = TestReport(
                        name=self.report_name,
                        duration=proc.end_to_end_time,
                        timestamp=datetime.now(),
                        commit_str=commit.commit_str,
                        commit=DBCommit.create_or_get(commit, session),
                        log=proc.output.replace("\x00", ""),
                        has_failed=has_failed
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
