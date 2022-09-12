# Copyright (c) 2019-present. The coop authors.
from contextlib import contextmanager

import click
from halo import Halo
from log_symbols import LogSymbols


def click_echo_success(message: str) -> None:
    """
    Echo success message to stdout.

    :param message:
    :return:
    """
    click.echo(
        "{} {}".format(LogSymbols.SUCCESS.value, click.style(message, fg="green"))
    )


def click_echo_failure(message: str) -> None:
    """
    Echo failure message to stdout.

    :param message:
    :return:
    """
    click.echo("{} {}".format(LogSymbols.ERROR.value, click.style(message, fg="red")))


def click_echo_info(message: str) -> None:
    """
    Echo informative message to stdout.

    :param message:
    :return:
    """
    click.echo("{} {}".format(LogSymbols.INFO.value, message))


def click_echo_warning(message: str) -> None:
    """
    Echo warning message to stdout.

    :param message:
    :return:
    """
    click.echo(
        "{} {}".format(LogSymbols.WARNING.value, click.style(message, fg="orange"))
    )


def start_spinner(text: str, spinner: str = "dots"):
    """
    Create new spinner and returns handle to it.
    Stop it by calling `.stop()`.

    :param text:
    :param spinner:
    :return:
    """
    spinner = Halo(text=text, spinner=spinner)
    spinner.start()
    return spinner


@contextmanager
def spinner_ctx(text: str, spinner: str = "dots"):
    """
    Create new spinner as contextmanager.
    """
    try:
        spinner = start_spinner(text=text, spinner=spinner)
        yield spinner
    finally:
        spinner.stop()
