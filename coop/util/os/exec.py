# Copyright (c) 2019-present. The coop authors.
import os
from pathlib import Path
from subprocess import STDOUT, Popen, TimeoutExpired
from time import time
from typing import Optional

from ..logging.logger import get_logger

_LOGGER = get_logger(__name__)

MS_EXE_FILE_SUFFIX: str = ".exe"


def is_executable_program(filepath: Path) -> bool:
    """
    Returns True if the given filepath is an executable program.

    :param filepath:
    :return:
    """
    return filepath.is_file() and os.access(filepath, os.X_OK)


def check_executable_exists(program: str) -> Optional[str]:
    """
    Check if provided program is an executable program, if yes, return executable file name or path it.

    :param program: Name of executable program (may be in short form)
    :return: None
    """
    executable: Optional[str] = None

    filepath, filename = os.path.split(program)
    if filepath:
        if is_executable_program(Path(program)):
            executable = program
        elif is_executable_program(Path(program + MS_EXE_FILE_SUFFIX)):
            executable = program
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            exe_file: str = os.path.join(path, program)
            if is_executable_program(Path(exe_file)):
                executable = exe_file
            elif is_executable_program(Path(exe_file + MS_EXE_FILE_SUFFIX)):
                executable = program

    return executable


def execute_command_as_subprocess(command: str, cwd: Optional[Path] = None) -> int:
    """
    Simple utility to execute commands as subprocesses.

    :param command: The command to execute.
    :param cwd: Working directory where to execute command.
    :return: Exit code.
    """
    _LOGGER.debug(f"Executing {command} in subprocess.")
    if cwd:
        p: Popen = Popen(command, shell=True, cwd=cwd)
    else:
        p: Popen = Popen(command, shell=True)
    exit_code = p.wait()
    return exit_code


class SubprocessContainer(object):
    """
    Simple wrapper for subprocesses.
    """

    def __init__(self, command: str, output_filepath: str) -> None:
        self.command = command
        self.output_filepath = output_filepath
        self.output: str = ""
        self.exit_code: int = -1
        self.end_to_end_time: float = -1

    def execute(self, capture_output: bool = False, timeout: Optional[float] = None, *args, **kwargs, ) -> None:
        # use `w+` to be able to read and write
        with open(self.output_filepath, "w+") as log_file:
            # pipe stdout/stderr into log file
            proc: Popen = Popen(
                self.command, stdout=log_file, stderr=STDOUT, *args, **kwargs
            )

            # run process and obtain exit code
            _LOGGER.debug("Executing: {}".format(self.command))
            start_time = time()
            if timeout is not None:
                try:
                    self.exit_code = proc.wait(timeout=timeout)
                except TimeoutExpired:
                    proc.kill()
            else:
                self.exit_code = proc.wait()
            end_time = time()
            self.end_to_end_time = end_time - start_time
            _LOGGER.debug("Exit code: {}".format(self.exit_code))
            _LOGGER.debug("E2E time: {:.2f}".format(self.end_to_end_time))

            # store output from execution
            if capture_output:
                _LOGGER.debug("Storing output.".format(self.exit_code))
                size = log_file.tell()
                log_file.seek(0)
                self.output = log_file.read(size)
