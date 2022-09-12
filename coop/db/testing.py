# Copyright (c) 2019-present. The coop authors.
from typing import List, Optional

from sqlalchemy import (
    Column,
    String,
    Integer,
    ForeignKey,
    DateTime,
    Float,
    Enum,
    Table,
    Text,
    UniqueConstraint,
    Boolean,
)
from sqlalchemy.orm import relationship, Session

from .base import Base
from .scm import DBCommit
from ..models.testing.base import TestReport, TestSuite, TestCase, TestStatus, DirtsDurationReport, StartsDurationReport


class DBTestReportMeta(Base.__class__, TestReport.__class__):
    ...


class DBTestSuiteMeta(Base.__class__, TestSuite.__class__):
    ...


class DBTestCaseMeta(Base.__class__, TestCase.__class__):
    ...


class DBDirtsDurationReportMeta(Base.__class__, DirtsDurationReport.__class__):
    ...

class DBStartsDurationReportMeta(Base.__class__, StartsDurationReport.__class__):
    ...


class DBTestReport(Base, TestReport, metaclass=DBTestReportMeta):
    name = Column(String, nullable=False)
    duration = Column(Float)
    timestamp = Column(DateTime)
    suites: List["DBTestSuite"] = relationship("DBTestSuite", back_populates="report")
    commit_str = Column(String, nullable=False)
    commit_id = Column(Integer, ForeignKey("{}.id".format(DBCommit.__tablename__),ondelete="CASCADE"))
    commit: DBCommit = relationship("DBCommit", back_populates="reports")
    log = Column(Text)
    has_failed = Column(Boolean)
    durations_dirts : List["DBDirtsDurationReport"] = relationship("DBDirtsDurationReport", back_populates="report")
    durations_starts: List["DBStartsDurationReport"] = relationship("DBStartsDurationReport", back_populates="report")

    __table_args__ = tuple(
        [UniqueConstraint("name", "commit_str", name="_name_revision_uc")]
    )

    @classmethod
    def get_single(
            cls, name: str, commit_str: str, session: Session
    ) -> Optional["DBTestReport"]:
        db_report: Optional[DBTestReport] = (
            session.query(DBTestReport).filter_by(name=name, commit_str=commit_str).first()
        )
        return db_report

    @classmethod
    def create_or_update(cls, report: TestReport, session: Session) -> "DBTestReport":
        # get report from DB
        db_report: Optional[DBTestReport] = cls.get_single(
            name=report.name, commit_str=report.commit_str, session=session
        )

        # create DB report object if not in DB yet
        if not db_report:
            # get commits if exist, otherwise create
            if report.commit:
                report.commit = DBCommit.create_or_get(report.commit, session)

            db_report = DBTestReport.from_domain(report)
            session.add(db_report)
        else:
            # if already existing, update all fields
            db_report.duration = report.duration if report.duration else db_report.duration

            db_report.timestamp = (
                report.timestamp if report.timestamp else db_report.timestamp
            )

            db_report.commit_str = report.commit_str if report.commit_str else db_report.commit_str
            # get from db if it exists
            db_report.commit = DBCommit.create_or_get(report.commit, session)
            db_report.suites = (
                [DBTestSuite.from_domain(s) for s in report.suites]
                if report.suites
                else db_report.suites
            )
            db_report.log = report.log if report.log else db_report.log
            db_report.has_failed = (
                report.has_failed
                if report.has_failed is not None
                else db_report.has_failed
            )
            db_report.durations_dirts = [DBDirtsDurationReport.from_domain(durations) for durations in
                                         report.durations_dirts] if report.durations_dirts else db_report.durations_dirts
            db_report.durations_starts = [DBStartsDurationReport.from_domain(durations) for durations in
                                         report.durations_starts] if report.durations_starts else db_report.durations_starts
        return db_report

    @classmethod
    def from_domain(cls, report: TestReport) -> "DBTestReport":
        if isinstance(report, cls) or not report:
            return report
        return cls(
            name=report.name,
            duration=report.duration,
            timestamp=report.timestamp,
            suites=[] if report.suites is None else [DBTestSuite.from_domain(suite) for suite in report.suites],
            commit_str=report.commit_str,
            commit=DBCommit.from_domain(report.commit),
            log=report.log,
            has_failed=report.has_failed,
            durations_dirts=[] if report.durations_dirts is None else [DBDirtsDurationReport.from_domain(durations) for
                                                                       durations in report.durations_dirts],
            durations_starts=[] if report.durations_starts is None else [DBStartsDurationReport.from_domain(durations) for
                                                                     durations in report.durations_starts]
        )

    def to_domain(self) -> TestReport:
        return TestReport(
            name=self.name,
            duration=self.duration,
            timestamp=self.timestamp,
            suites=[DBTestSuite.to_domain(suite) for suite in self.suites],
            commit_str=self.commit_str,
            commit=self.commit.to_domain(),
            log=self.log,
            has_failed=self.has_failed,
            durations_dirts=self.durations_dirts,
            durations_starts=self.durations_starts
        )

class DBTestSuite(Base, TestSuite, metaclass=DBTestSuiteMeta):
    name = Column(String, nullable=False)
    duration = Column(Float)
    total_count = Column(Integer)
    pass_count = Column(Integer)
    fail_count = Column(Integer)
    skip_count = Column(Integer)
    error_count = Column(Integer)
    report_id = Column(Integer, ForeignKey("{}.id".format("dbtestreport"),ondelete="CASCADE"))
    report = relationship("DBTestReport", back_populates="suites")
    cases: List["DBTestCase"] = relationship("DBTestCase", back_populates="suite", cascade="all, delete-orphan")

    @classmethod
    def from_domain(cls, suite: TestSuite) -> "DBTestSuite":
        if isinstance(suite, cls) or not suite:
            return suite
        return cls(
            name=suite.name,
            duration=suite.duration,
            cases=[DBTestCase.from_domain(case) for case in suite.cases],
            total_count=suite.total_count,
            pass_count=suite.pass_count,
            fail_count=suite.fail_count,
            skip_count=suite.skip_count,
            error_count=suite.error_count,
        )

    def to_domain(self) -> TestSuite:
        return TestSuite(
            name=self.name,
            duration=self.duration,
            cases=[c.to_domain() for c in self.cases],
            total_count=self.total_count,
            pass_count=self.pass_count,
            fail_count=self.fail_count,
            skip_count=self.skip_count,
            error_count=self.error_count,
        )

class DBTestCase(Base, TestCase, metaclass=DBTestCaseMeta):
    name = Column(String, nullable=True)
    duration = Column(Float)
    status = Column(Enum(TestStatus))
    suite_id = Column(Integer, ForeignKey("{}.id".format(DBTestSuite.__tablename__) ,ondelete="CASCADE"))
    suite = relationship("DBTestSuite", back_populates="cases")
    error_stacktrace = Column(String)

    @classmethod
    def from_domain(cls, case: TestCase) -> "DBTestCase":
        if isinstance(case, cls) or not case:
            return case
        return cls(
            name=case.name,
            duration=case.duration,
            status=case.status,
            error_stacktrace=case.error_stacktrace,
        )

    def to_domain(self) -> TestCase:
        return TestCase(
            name=self.name,
            duration=self.duration,
            status=self.status,
            error_stacktrace=self.error_stacktrace,
        )


class DBDirtsDurationReport(Base, DirtsDurationReport, metaclass=DBDirtsDurationReportMeta):
    report_id = Column(Integer, ForeignKey("{}.id".format(DBTestReport.__tablename__), ondelete="CASCADE"))
    report = relationship(DBTestReport, back_populates="durations_dirts")
    duration_project_importer = Column(Float)
    duration_typesolver_initializer = Column(Float)
    duration_parser = Column(Float)
    duration_test_finder = Column(Float)
    duration_change_analyzer = Column(Float)
    duration_graph_cropper = Column(Float)
    duration_dependency_analyzer = Column(Float)
    duration_graph_combiner = Column(Float)
    duration_project_exporter = Column(Float)
    duration_all = Column(Float)

    @classmethod
    def from_domain(cls, report: DirtsDurationReport) -> "DBDirtsDurationReport":
        if isinstance(report, cls) or not report:
            return report
        return cls(
            duration_project_importer=report.duration_project_importer,
            duration_typesolver_initializer=report.duration_typesolver_initializer,
            duration_parser=report.duration_parser,
            duration_test_finder=report.duration_test_finder,
            duration_change_analyzer=report.duration_change_analyzer,
            duration_graph_cropper=report.duration_graph_cropper,
            duration_dependency_analyzer=report.duration_dependency_analyzer,
            duration_graph_combiner=report.duration_graph_combiner,
            duration_project_exporter=report.duration_project_exporter,
            duration_all=report.duration_all,
        )

    def to_domain(self) -> DirtsDurationReport:
        return DirtsDurationReport(
            duration_project_importer=self.duration_project_importer,
            duration_typesolver_initializer=self.duration_typesolver_initializer,
            duration_parser=self.duration_parser,
            duration_test_finder=self.duration_test_finder,
            duration_change_analyzer=self.duration_change_analyzer,
            duration_graph_cropper=self.duration_graph_cropper,
            duration_dependency_analyzer=self.duration_dependency_analyzer,
            duration_graph_combiner=self.duration_graph_combiner,
            duration_project_exporter=self.duration_project_exporter,
            duration_all=self.duration_all,
        )


class DBStartsDurationReport(Base, StartsDurationReport, metaclass=DBStartsDurationReportMeta):
    report_id = Column(Integer, ForeignKey("{}.id".format(DBTestReport.__tablename__), ondelete="CASCADE"))
    report = relationship(DBTestReport, back_populates="durations_starts")
    duration_jdeps = Column(Float)
    duration_graph = Column(Float)
    duration_all = Column(Float)

    @classmethod
    def from_domain(cls, report: StartsDurationReport) -> "DBStartsDurationReport":
        if isinstance(report, cls) or not report:
            return report
        return cls(
            duration_jdeps=report.duration_jdeps,
            duration_graph=report.duration_graph,
            duration_all=report.duration_all
        )

    def to_domain(self) -> StartsDurationReport:
        return StartsDurationReport(
            duration_jdeps=self.duration_jdeps,
            duration_graph=self.duration_graph,
            duration_all=self.duration_all
        )