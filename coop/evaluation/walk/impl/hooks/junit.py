# Copyright (c) 2019-present. The coop authors.
from datetime import datetime
from typing import Optional, List

from ...base import Hook
from .....db.base import DBConnection
from .....db.scm import DBCommit
from .....db.testing import DBTestReport
from .....models.scm.base import Commit, Repository
from .....models.testing.base import TestReport
from .....models.testing.impl.junit import JUnitTestReportLoader
from .....models.testing.loader import TestReportLoader
from .....util.logging.logger import get_logger

_LOGGER = get_logger(__name__)


class JUnitResultsToDBHook(Hook):
    def __init__(
            self,
            repository: Repository,
            connection: DBConnection,
            report_name: str,
            output_path: Optional[str] = None,
    ) -> None:
        super().__init__(repository, output_path)
        self.connection = connection
        self.report_name = report_name

    def run(self, commit: Commit) -> bool:
        with self.connection.create_session_ctx() as session:
            # get DB commit object from DB if exists
            db_commit = DBCommit.create_or_get(commit=commit, session=session)

            # create result loader
            report_loader: TestReportLoader = JUnitTestReportLoader(
                path=self.repository.path,
                commit_str=db_commit.commit_str,
                commit=db_commit,
            )
            # load report by searching filesystem tree under repository root
            report: Optional[TestReport] = report_loader.load()

            # create empty report if none found
            if report is None:
                _LOGGER.debug(
                    "No JUnit test report could be found under {}.".format(
                        self.repository.path
                    )
                )
                report = TestReport(commit=db_commit,
                                    commit_str=db_commit.commit_str,
                                    name=self.report_name,
                                    duration=0.0,
                                    timestamp=datetime.now(),
                                    suites=[])

            report.name = self.report_name
            # get potentially existing report from DB
            db_report: Optional[DBTestReport] = DBTestReport.get_single(
                name=report.name, commit_str=report.commit_str, session=session
            )
            # if existing, copy attributes that have *not* been set by the junit report loader
            if db_report:
                report.duration = db_report.duration
                report.timestamp = db_report.timestamp
                report.log = db_report.log
                report.has_failed = db_report.has_failed
            # update or create test report
            DBTestReport.create_or_update(report=report, session=session)
            # commit object
            session.commit()
        return True
