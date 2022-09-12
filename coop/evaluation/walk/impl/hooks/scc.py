# Copyright (c) 2019-present. The coop authors.
import json
import os
from typing import Optional
from time import time

from coop.db.testing import DBTestReport

from ...base import Hook
from .....db.base import DBConnection
from .....db.scm import DBCommit
from .....models.scm.base import Commit, Repository
from .....util.logging.logger import get_logger
from .....util.os.exec import SubprocessContainer

_LOGGER = get_logger(__name__)


class SccHook(Hook):
    def __init__(
            self,
            repository: Repository,
            connection: DBConnection,
            output_path: Optional[str] = None
    ) -> None:
        super().__init__(repository, None)
        self.connection = connection
        if self.output_path:
            self.cache_dir = os.path.join(self.output_path, ".scc-hook")
        else:
            self.cache_dir = os.path.join(self.repository.path, ".scc-hook")

    def run(self, commit: Commit) -> bool:
        with self.connection.create_session_ctx() as session:
            # get DB commit object from DB if exists
            db_commit = DBCommit.create_or_get(commit=commit, session=session)

            os.makedirs(self.cache_dir, exist_ok=True)

            # prepare cache dir/file
            cache_file = "run_{}.log".format(
                int(time() * 1000)
            )  # run identified by timestamp
            cache_file_path = os.path.join(self.cache_dir, cache_file)

            command = "scc " + self.repository.path + " -f json"

            proc: SubprocessContainer = SubprocessContainer(
                command=command, output_filepath=cache_file_path
            )
            proc.execute(capture_output=True, shell=True, timeout=1000.0)

            result = json.loads(proc.output)

            java = list(filter(lambda x: x["Name"] == "Java", result))

            if java:
                loc_java = java[0]["Code"]
                files_java = java[0]["Count"]

                commit.files_java = files_java
                commit.loc_java = loc_java

            else:
                commit.files_java = 0
                commit.loc_java = 0

            DBCommit.create_or_update(commit=commit, session=session)
            # commit object
            session.commit()

        return True
