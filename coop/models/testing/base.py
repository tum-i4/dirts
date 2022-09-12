"""
Module for general testing classes used for testing optimization algorithms.
"""
# Copyright (c) 2019-present. The coop authors.

from datetime import datetime
from enum import Enum
from typing import List, Union, Optional, Dict

from ...models.scm.base import Commit


class TestEvent(str, Enum):
    """
    An enum for different test execution events.
    """

    SUITE_STARTED = "SUITE_STARTED"
    TEST_STARTED = "TEST_STARTED"
    TEST_FINISHED = "TEST_FINISHED"
    SUITE_FINISHED = "SUITE_FINISHED"


class TestStatus(Enum):
    """
    An enum for different statuses/results of test cases/suites.
    """

    PASSED = "PASSED"
    FAILED = "FAILED"
    SKIPPED = "SKIPPED"
    IGNORED = "IGNORED"
    EXCLUDED = (
        "EXCLUDED"  # this is a status which coop will assign to non-selected test cases
    )
    UNDEFINED = "UNDEFINED"
    ERRORED = "ERRORED"


class TestCase(object):
    """
    A test case is a single test and is from an implementation perspective
    often a test methods inside a class (suite) with multiple test cases.
    """

    def __init__(
            self,
            name: str,
            duration: float = 0.0,
            status: TestStatus = TestStatus.UNDEFINED,
            error_stacktrace: Optional[str] = None,
    ) -> None:
        """
        Constructor for test cases

        :param name: Unique identifier for test case (e.g. the precise class name including the package + method name)
        :param duration: Duration of case execution in seconds
        :param status: Status of the test case (i.e. passed, failed, skipped, ignored)
        :param error_stacktrace: Error stacktrace
        """
        self.name = name
        self.duration = duration
        self.status = status
        self.error_stacktrace = error_stacktrace

    def __eq__(self, o: "TestCase") -> bool:
        """
        Equivalence check (within test suite)

        :param o:
        :return:
        """
        return self.name == o.name

    def __hash__(self) -> int:
        """
        Test case is hashable with its name only

        :return:
        """
        return hash(self.name)

    def __repr__(self) -> str:
        """
        Print name.

        :return:
        """
        return self.name

    @classmethod
    def from_dict(cls, test_case: Dict) -> "TestCase":
        return cls(
            name=test_case["name"],
            duration=test_case["duration"],
            status=TestStatus[test_case["status"]],
            error_stacktrace=test_case["error_stacktrace"],
        )


class TestSuite(object):
    """
    A test suite contains one or more test cases and is from an implementation perspective
    often a class with multiple test methods.
    """

    def __init__(
        self,
        name: str,
        duration: float,
        cases: List[TestCase],
        total_count: Optional[int] = None,
        pass_count: Optional[int] = None,
        fail_count: Optional[int] = None,
        skip_count: Optional[int] = None,
        error_count: Optional[int] = None,
        meta_data: Optional[str] = None
    ) -> None:
        """
        Constructor for test suites

        :param name: Unique identifier for test suite (e.g. the precise class name including the package)
        :param duration: Duration of suite execution in seconds
        :param cases: List of test cases contained in suite
        :param total_count: Count of test cases
        :param pass_count: Count of passes
        :param fail_count: Count of failures
        :param skip_count: Count of skips
        :param error_count: Count of errors
        :param meta_data: Metadata for the test suite
        """
        self.name = name
        self.duration = duration
        self.cases = cases
        self._total_count = total_count
        self._pass_count = pass_count
        self._fail_count = fail_count
        self._skip_count = skip_count
        self._error_count = error_count
        self.meta_data = meta_data

    @property
    def total_count(self) -> int:
        if self._total_count:
            return self._total_count
        return len(self.cases)

    @property
    def pass_count(self) -> int:
        if self._pass_count:
            return self._pass_count
        return len(self.get_filtered_cases(status=TestStatus.PASSED))

    @property
    def fail_count(self) -> int:
        if self._fail_count:
            return self._fail_count
        return len(self.get_filtered_cases(status=TestStatus.FAILED))

    @property
    def skip_count(self) -> int:
        if self._skip_count:
            return self._skip_count
        return len(self.get_filtered_cases(status=TestStatus.SKIPPED))

    @property
    def error_count(self) -> int:
        if self._error_count:
            return self._error_count
        return len(self.get_filtered_cases(status=TestStatus.ERRORED))

    def get_setup_time(self) -> float:
        return self.duration - sum([tc.duration for tc in self.cases])

    def get_filtered_cases(self, status: TestStatus) -> List[TestCase]:
        return list(filter(lambda tc: tc.status == status, self.cases))

    @property
    def error_stacktrace(self) -> str:
        return ",".join([tc.error_stacktrace for tc in self.cases])

    def __eq__(self, o: "TestSuite") -> bool:
        """
        Equivalence check (within test report)

        :param o:
        :return:
        """
        return self.name == o.name

    def __hash__(self) -> int:
        return hash(self.name)

    def __lt__(self, other: "TestSuite") -> bool:
        return self.name < other.name

    @classmethod
    def from_dict(cls, test_suite: Dict) -> "TestSuite":
        # we support two different kinds of JSON schemas here (one from the `ttrace` project, one from `coop`)
        return cls(
            name=test_suite["testId" if "testId" in test_suite else "name"],
            duration=test_suite["duration"],
            cases=(
                list(map(lambda tc: TestCase.from_dict(tc), test_suite["cases"]))
                if "cases" in test_suite
                else []
            ),
            total_count=(
                test_suite["_total_count"] if "_total_count" in test_suite else None
            ),
            pass_count=(
                test_suite["passed"]
                if "passed" in test_suite
                else test_suite["_pass_count"]
            ),
            fail_count=(
                test_suite["failed"]
                if "failed" in test_suite
                else test_suite["_fail_count"]
            ),
            skip_count=(
                test_suite["skipped"]
                if "skipped" in test_suite
                else test_suite["_skip_count"]
            ),
            error_count=(
                test_suite["_error_count"] if "_error_count" in test_suite else None
            ),
        )


class DirtsDurationReport(object):
    def __init__(
            self,
            duration_project_importer: float,
            duration_typesolver_initializer: float,
            duration_parser: float,
            duration_test_finder: float,
            duration_change_analyzer: float,
            duration_dependency_analyzer: float,
            duration_graph_cropper: float,
            duration_graph_combiner: float,
            duration_project_exporter: float,
            duration_all: float,
    ) -> None:
        self.duration_project_importer = duration_project_importer
        self.duration_typesolver_initializer = duration_typesolver_initializer
        self.duration_parser = duration_parser
        self.duration_test_finder = duration_test_finder
        self.duration_change_analyzer = duration_change_analyzer
        self.duration_dependency_analyzer = duration_dependency_analyzer
        self.duration_graph_cropper = duration_graph_cropper
        self.duration_graph_combiner = duration_graph_combiner,
        self.duration_project_exporter = duration_project_exporter
        self.duration_all = duration_all

class StartsDurationReport(object):
    def __init__(
            self,
            duration_jdeps: float,
            duration_graph: float,
            duration_all: float
    ) -> None:
        self.duration_jdeps = duration_jdeps
        self.duration_graph = duration_graph
        self.duration_all = duration_all


class TestReport(object):
    """
    A test report encapsulates the results of the execution of an entire test set.
    It contains test suites, which in turn contain test cases.
    """

    def __init__(
            self,
            name: str,
            duration: float,
            timestamp: datetime,
            suites: List[TestSuite] = None,
            durations_dirts=None,
            durations_starts=None,
            commit_str: Union[Optional[str], Optional[int]] = None,
            commit: Commit = None,
            log: Optional[str] = None,
            has_failed: Optional[bool] = None,
    ) -> None:
        """
        Constructor for test reports

        :param name: Unique identifier for test report (e.g. the build id)
        :param duration: Duration of complete testing procedure in seconds
        :param timestamp: Timestamp of test report, necessary for sorting
        :param suites: List of test suites contained in report
        :param commit: SCM revision of test report
        :param log: Execution log
        :param has_failed: Boolean flag if exit code != 0
        """
        if durations_dirts is None:
            durations_dirts = []
        if durations_starts is None:
            durations_starts = []
        self.name = name
        self.duration = duration
        self.timestamp = timestamp
        self.suites = suites
        self.durations_dirts = durations_dirts
        self.durations_starts = durations_starts
        self.commit_str = commit_str
        self.commit = commit
        self.log = log
        self.has_failed = has_failed

    def get_filtered_cases(self, status: TestStatus) -> List[TestCase]:
        cases = []
        for suite in self.suites:
            cases += suite.get_filtered_cases(status)
        return cases

    def __eq__(self, o: "TestReport") -> bool:
        return self.name == o.name

    def __lt__(self, other: "TestReport") -> bool:
        return self.timestamp < other.timestamp
