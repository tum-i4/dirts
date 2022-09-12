# Copyright (c) 2019-present. The coop authors.
from contextlib import contextmanager
from typing import Optional, List

from sqlalchemy import create_engine, Integer, Column, DateTime, func
from sqlalchemy.engine import Engine, Connection
from sqlalchemy.ext.declarative import declared_attr, as_declarative
from sqlalchemy.orm import sessionmaker, Session

from ..util.logging.logger import get_logger

_LOGGER = get_logger(__name__)


@as_declarative()
class Base(object):
    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
    __name__: str

    @declared_attr
    def __tablename__(cls) -> str:
        return cls.__name__.lower()


# noinspection PyUnresolvedReferences
from . import scm, testing  # necessary to create schema


class DBConnection(object):
    def __init__(self, url: str, *args, **kwargs) -> None:
        super().__init__()
        self.url: str = url
        self.engine: Engine = create_engine(url, *args, **kwargs)
        self.Session: sessionmaker = sessionmaker(bind=self.engine, expire_on_commit=False)

    def create_session(self) -> Session:
        return self.Session()

    @contextmanager
    def create_session_ctx(self, expunge_all: bool = True) -> Session:
        session = self.create_session()
        try:
            yield session
        finally:
            if expunge_all:
                session.expunge_all()
            session.close()

    def terminate_all_sessions(self) -> None:
        self.Session.close_all()

    def create_schema(self) -> None:
        self.terminate_all_sessions()
        _LOGGER.debug(
            "Creating schema with tables: {}".format(Base.metadata.tables.keys())
        )
        Base.metadata.create_all(self.engine)

    def delete_schema(self) -> None:
        self.terminate_all_sessions()
        _LOGGER.debug(
            "Dropping schema with tables: {}".format(Base.metadata.tables.keys())
        )
        Base.metadata.drop_all(self.engine)

    def get_tables(self) -> List[str]:
        return self.engine.table_names()
