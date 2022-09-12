# Copyright (c) 2019-present. The coop authors.
import os
from datetime import datetime
from glob import glob
from typing import List, Dict, Optional, Union

from junitparser import JUnitXml, junitparser

from ..base import TestReport, TestSuite, TestCase, TestStatus
from ..loader import TestReportLoader
from ...scm.base import Commit

# https://github.com/gastlygem/junitparser/blob/master/junitparser/junitparser.py#L538
JUNIT_STATUS_MAPPING: Dict[str, TestStatus] = {
    "skipped": TestStatus.SKIPPED,
    "error": TestStatus.ERRORED,
    "failure": TestStatus.FAILED,
}


class JUnitTestReportLoader(TestReportLoader):
    def __init__(
        self,
        path: str,
        pattern: str = "**/TEST-*.xml",
        commit_str: Optional[str] = None,
        commit: Optional[Commit] = None,
    ) -> None:
        """
        Constructor.

        :param path: Root directory containing JUnit test results, e.g., from multiple test runs.
        :param pattern: Glob pattern to find JUnit xml files.
        :param commit_str:
        :param commit:
        """
        super().__init__()
        self.path = path
        self.pattern = pattern
        self.commit_str = commit_str
        self.commit = commit

    def load(self, n: int = -1, *args, **kwargs) -> Optional[TestReport]:
        test_reports: TestReport

        test_suite_files: List[str] = glob(
            os.path.join(self.path, self.pattern), recursive=True
        )
        if len(test_suite_files) > 0:
            test_suites: List[TestSuite] = []
            merge_xml: Optional[JUnitXml] = None
            first_timestamp: Optional[datetime] = None
            for filepath in test_suite_files:
                # parse junit test suite result file
                xml: Union[JUnitXml, junitparser.TestSuite] = JUnitXml.fromfile(
                    filepath
                )

                # if there are multiple test suites included, iterate over them
                if isinstance(xml, JUnitXml):
                    # get all suites and cases
                    for suite in xml:
                        # create test suite from xml
                        test_suites.append(self._get_test_suite_from_xml(suite))
                        # set first timestamp
                        if first_timestamp is None:
                            first_timestamp = suite.timestamp
                else:
                    # if there is only one test suite, add it to the list of suites
                    test_suites.append(self._get_test_suite_from_xml(xml))
                    # set first timestamp
                    if first_timestamp is None:
                        first_timestamp = xml.timestamp

                # merge xml files for test report
                if merge_xml is None:
                    merge_xml = xml
                else:
                    merge_xml += xml

                # remove xml file
                if os.path.exists(filepath):
                    os.remove(filepath)

            # create test report object
            test_report: TestReport = TestReport(
                name="",
                duration=merge_xml.time,
                timestamp=first_timestamp,
                suites=test_suites,
            )
            test_report.commit = self.commit
            test_report.commit_str = self.commit_str
            return test_report
        else:
            return None


    def _get_test_status(self, result: List[junitparser.Result]) -> TestStatus:
        if any([r for r in result if isinstance(r, junitparser.Failure)]):
            return TestStatus.FAILED
        elif any([r for r in result if isinstance(r, junitparser.Error)]):
            return TestStatus.ERRORED
        if any([r for r in result if isinstance(r, junitparser.Skipped)]):
            return TestStatus.SKIPPED
        return TestStatus.PASSED

    def _get_error_stack_trace(self, result: List[junitparser.Result]) -> str:
        messages = ""
        for r in result:
            if isinstance(r, junitparser.Error) and r.message:
                messages += str(r.message)
        return messages

    def _get_test_suite_from_xml(self, xml: junitparser.TestSuite) -> TestSuite:
        test_suite: TestSuite = TestSuite(
            name=xml.name,
            duration=xml.time,
            cases=[
                TestCase(
                    name=case.name,
                    duration=case.time,
                    status=self._get_test_status(case.result),
                    error_stacktrace=self._get_error_stack_trace(case.result)
                )
                for case in xml
            ],
            total_count=xml.tests,
            pass_count=(xml.tests - xml.failures - xml.skipped - xml.errors),
            fail_count=xml.failures,
            skip_count=xml.skipped,
            error_count=xml.errors,
        )
        return test_suite
