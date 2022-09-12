"""
Module for loaders required to load objects from remote (CI) servers or from the filesystem.
"""
# Copyright (c) 2019-present. The coop authors.
from abc import ABC, abstractmethod
from typing import List, Optional

from .base import TestReport


class TestReportLoader(ABC):
    """
    A generic loader interface to load test reports from different data sources.
    An example is the JenkinsTestReportLoader in `models.testing.impl.jenkins`.
    """

    @abstractmethod
    def load(self, n: int = -1, *args, **kwargs) -> Optional[TestReport]:
        pass
