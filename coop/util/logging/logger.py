# Copyright (c) 2019-present. The coop authors.
import logging
import sys

logging.basicConfig(
    format="[%(process)d] %(levelname)s: %(asctime)s: %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
    level=logging.INFO,
    stream=sys.stdout,
)


def get_logger(name: str) -> logging.Logger:
    """
    Get logger by name (e.g., filename).

    :param name: Name for logger (e.g., `__name__`).
    :return:
    """
    return logging.getLogger(name)


def configure_logging_verbosity(verbosity: int) -> None:
    """
    Set the maximum verbosity for all loggers.

    :param verbosity: Verbosity level.
    """
    logging.getLogger().setLevel(verbosity)
