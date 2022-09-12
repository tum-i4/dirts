"""
This module contains hooks for using the tool Ekstazi in repository walkers.
https://github.com/gliga/ekstazi
"""
# Copyright (c) 2019-present. The coop authors.
import glob
import os
import re
import xml.etree.ElementTree as ET
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
from .....models.testing.base import TestReport, StartsDurationReport
from .....util.os.exec import check_executable_exists, SubprocessContainer

_LOGGER = get_logger(__name__)


class EkstaziMavenHook(Hook):
    def __init__(
            self,
            repository: Repository,
            git_client: GitClient,
            output_path: Optional[str] = None,
            java_version: str = "",
            cmd: str = "org.ekstazi:ekstazi-maven-plugin:5.3.0:",
            clean: bool = True,
            options=None,
            report_name: Optional[str] = None,
            connection: Optional[DBConnection] = None,
            java11_special: bool = False
    ) -> None:
        super().__init__(repository, output_path, git_client)
        if options is None:
            options = ["-Dforcefailing=false",
                       "-Dmaven.test.failure.ignore=true",
                       "-e"]
        if self.output_path:
            self.cache_dir = os.path.join(self.output_path, ".ekstazi-hook")
        else:
            self.cache_dir = os.path.join(self.repository.path, ".ekstzai-hook")
        self.java_version = java_version
        self.executable: str = "mvn"
        if not check_executable_exists(self.executable):
            raise Exception("Executable {} does not exist".format(self.executable))
        self.cmd = cmd
        self.clean = clean
        self.options = options
        self.report_name = report_name
        self.connection = connection
        self.add_javax_annotations_dependency = java11_special

    def _update_pom_special(self, plugins_node):

        # append configuration of ekstazi
        plugin = self._search_artifact_group(plugins_node, "plugin", "ekstazi-maven-plugin", "org.ekstazi")
        version = self._search_node(plugin, "version")
        version.text = "5.3.0"

        configurations_node = self._search_node(plugin, "configuration")
        force_failing_node = self._search_node(configurations_node, "forcefailing")
        force_failing_node.text = "false"

        executions_node = self._search_node(plugin, "executions")
        plugin.remove(executions_node)
        executions_node = ET.Element("executions")
        plugin.append(executions_node)
        execution_node = ET.Element("execution")
        id_node = ET.Element("id")
        id_node.text = "ekstazi"
        execution_node.append(id_node)
        goals_node = ET.Element("goals")
        goal_restore_node = ET.Element("goal")
        goal_restore_node.text = "restore"
        goals_node.append(goal_restore_node)
        goal_select_node = ET.Element("goal")
        goal_select_node.text = "select"
        goals_node.append(goal_select_node)
        execution_node.append(goals_node)
        executions_node.append(execution_node)


    def _run_ekstazi(self, commit: Commit) -> bool:
        """
        Run Ekstazi tool.

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

        # run ekstazi:ekstazi to generate temporary files
        command: str = "{0}{1} {2}ekstazi {3}".format(
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
        self.git_client.git_repo.git.reset(commit.commit_str, hard=True)

        has_failed |= self._update_pom()

        # prepare command, no clean here
        command: str = "{0}{1} {2}ekstazi {3}".format(
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
        failures = re.search("(The forked VM terminated without properly saying goodbye. VM crash or System.exit called?)", proc.output)
        has_failed |= failures is not None and len(failures.groups()) != 0

        if self.connection and self.report_name:
            try:
                with self.connection.create_session_ctx() as session:
                    report = TestReport(
                        name=self.report_name,
                        duration=proc.end_to_end_time,
                        timestamp=datetime.now(),
                        commit=commit,
                        commit_str=commit.commit_str,
                        log=proc.output.replace("\x00", ""),
                        has_failed=has_failed,
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
        return self._run_ekstazi(commit)

    def getExcludesFilePath(self, pom_path):
        return os.path.join(Path(pom_path).parent, "EkstaziExcludes")
