# Copyright (c) 2019-present. The coop authors.
import os
import shutil
import tempfile
from contextlib import contextmanager
from typing import Optional, Union, Generator

from ..logging.logger import get_logger

_LOGGER = get_logger(__name__)


@contextmanager
def temp_path():
    """
    Create temporary directory, navigate into it and yield path to it.
    On deletion of context (i.e. `with` clause), navigate back to original path and remove temporary directory.
    """
    original_wd: str = os.getcwd()

    tmp_path: Union[Optional[str], Optional[bytes]] = None
    try:
        tmp_path = tempfile.mkdtemp()
        os.chdir(tmp_path)

        yield tmp_path
    finally:
        os.chdir(original_wd)

        if tmp_path is not None and os.path.exists(tmp_path) is True:
            try:
                shutil.rmtree(tmp_path)
            except Exception as e:
                _LOGGER.debug("Exception occurred when removing temporary path {}.".format(tmp_path))
                _LOGGER.debug(e)
                pass
