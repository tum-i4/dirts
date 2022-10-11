# Copyright (c) 2022-present. The coop authors.
import argparse
import logging
import shutil
import tempfile

from git import Repo

from coop.db.base import DBConnection
from coop.evaluation.walk.impl.git_walker import GitWalker
from coop.evaluation.walk.impl.hooks.dirts import DIRTSMavenHook
from coop.evaluation.walk.impl.hooks.ekstazi import EkstaziMavenHook
from coop.evaluation.walk.impl.hooks.junit import JUnitResultsToDBHook
from coop.evaluation.walk.impl.hooks.maven import MavenHook
from coop.evaluation.walk.impl.hooks.scc import SccHook
from coop.evaluation.walk.impl.hooks.starts import STARTSMavenHook
from coop.models.scm.base import Repository, DependencyInjectionType
from coop.models.scm.impl.git import GitClient
from coop.util.logging.logger import configure_logging_verbosity

di_related_changes_guice = [
    "@Inject",
    "@Provides",
    "Provider<",
    ".bind(",
    "@ImplementedBy",
    "@ProvidedBy",
    ".addBinding(",
    ".to(",
    ".toInstance(",
    ".getInstance",
    "@AutoBindSingleton",
    ".getInstance("]

di_related_changes_cdi = [
    "@Inject",
    "@Alternative",
    "select(",
    "@Priority",
    "<class",
    "<alternatives",
    "@Named",
    "@Any",
    "@Default",
    "@Disposes,"
    "@Produces"
]

di_related_changes_spring = [
    "@Autowired",
    "@Bean",
    "<bean",
    "@Resource",
    "@Configuration",
    "@Component",
    "@Primary",
    "@ComponentScan",
    "@Qualifier",
    "@Named",
    "@Required",
    "@Inject",
    "@Repository",
    "@Service",
    "@Controller",
    "getBean(",
    "getBeansOfType("
]


def main():
    # define program arguments
    parser = argparse.ArgumentParser()
    parser.add_argument("path", nargs="?")
    parser.add_argument(
        "--db_url",
        default="postgresql://localhost:5432/dirts", # TODO: adapt
        help="DB connection string",
    )
    parser.add_argument(
        "--commit_list", "-c", default="", help="file containing all commits that should be walked"
    )
    parser.add_argument("--branch", "-b", default="master")
    parser.add_argument(
        "--num_commits", "-n", type=int, default=1, help="amount of walked commits"
    )
    parser.add_argument(
        "--output", "-o", default="", help="Output path for created reports etc."
    )
    parser.add_argument(
        "--logging",
        "-l",
        default="DEBUG",
        help="Logging level: DEBUG, INFO, WARNING, ERROR, CRITICAL",
    )

    parser.add_argument("--options", help="Can be used to specify additional options")
    parser.add_argument(
        "--guice", action='store_true', help="To specify that Guice is used for dependency injection"
    )
    parser.add_argument(
        "--spring", action='store_true', help="To specify that Spring is used for dependency injection"
    )
    parser.add_argument(
        "--cdi", action='store_true', help="To specify that CDI is used for dependency injection"
    )

    args = parser.parse_args()

    # set logging level
    numeric_level = getattr(logging, args.logging.upper(), None)
    if not isinstance(numeric_level, int):
        raise ValueError(f"Invalid log level: {args.logging}")
    configure_logging_verbosity(numeric_level)

    # if you want to clone a remote repository
    path = args.path
    tmp_path = None
    if ".git" in path:
        tmp_path = tempfile.mkdtemp()
        Repo.clone_from(url=path, to_path=tmp_path)
        path = tmp_path

    # Collect tokens indicating changes related to dependency injection
    di_related_changes = []
    di_types = []
    di_cmd = ""
    if args.guice:
        di_related_changes.extend(di_related_changes_guice)
        di_types.append(DependencyInjectionType.GUICE)
        di_cmd += " -DuseGuiceExtension=True"
    if args.spring:
        di_related_changes.extend(di_related_changes_spring)
        di_types.append(DependencyInjectionType.SPRING)
        di_cmd += " -DuseSpringExtension=True"
    if args.cdi:
        di_related_changes.extend(di_related_changes_cdi)
        di_types.append(DependencyInjectionType.CDI)
        di_cmd += " -DuseCDIExtension=True"

    # create DB connection
    connection = DBConnection(url=args.db_url)

    # create repo
    repository = Repository(path=path, repository_type="git", di_types=di_types)
    git_client = GitClient(repository)

    options = args.options if args.options else ""

    # TODO: adapt
    java11 = "JAVA_HOME=/usr/lib/jvm/java-11-openjdk"
    java8 = "JAVA_HOME=/usr/lib/jvm/java-8-openjdk"

    # If a commit is added to the repositories, the seed responsible for making the evaluation reproducible
    # does not work correctly anymore
    # that is why we fixed the commits that are analyzed
    commit_list = None
    if args.commit_list != "":
        f = open(args.commit_list, "r")
        lines = f.readlines()
        commit_list = lines

    walker = GitWalker(
        randomize=True,
        repository=repository,
        branch=args.branch,
        connection=connection,
        commit_list=commit_list,
        num_commits=args.num_commits,
        search_terms=di_related_changes,

        hooks=[
            # scc
            SccHook(
                repository=repository,
                connection=connection
            ),

            # retest-all
            MavenHook(
                repository=repository,
                git_client=git_client,
                report_name="Retest-all",
                java_version=java8,
                connection=connection,
                clean=True,
                options=["-Dmaven.test.failure.ignore=true", "-e", options]
            ),
            JUnitResultsToDBHook(
                repository=repository,
                connection=connection,
                report_name="Retest-all"
            ),

            # starts
            STARTSMavenHook(
                repository=repository,
                git_client=git_client,
                report_name="STARTS",
                connection=connection,
                output_path=args.output,
                java_version=java8,
                options=["-DstartsLogging=FINEST", "-Dmaven.test.failure.ignore=true", "-e", options]
            ),
            JUnitResultsToDBHook(
                repository=repository,
                connection=connection,
                report_name="STARTS"
            ),

            # # ekstazi
            # EkstaziMavenHook(
            #     repository=repository,
            #     git_client=git_client,
            #     report_name="Ekstazi",
            #     connection=connection,
            #     output_path=args.output,
            #     java_version=java11,
            #     options=["-Dmaven.test.failure.ignore=true", "-e", options],
            #     add_javax_annotations_dependency=True,
            # ),
            # JUnitResultsToDBHook(
            #     repository=repository,
            #     connection=connection,
            #     report_name="Ekstazi"
            # ),

            # dirts standalone class level
            DIRTSMavenHook(
                repository=repository,
                git_client=git_client,
                report_name="DIRTS_class_level_standalone",
                connection=connection,
                output_path=args.output,
                java_version=java11,
                type="class_level_select",
                options=["-Dmaven.test.failure.ignore=true", "-Dstandalone=true", "-Dlogging=FINER", "-e", di_cmd, options],
                java11_special=True
            ),
            JUnitResultsToDBHook(
                repository=repository,
                connection=connection,
                report_name="DIRTS_class_level_standalone"
            ),

            # dirts standalone method level
            DIRTSMavenHook(
                repository=repository,
                git_client=git_client,
                report_name="DIRTS_method_level_standalone",
                connection=connection,
                output_path=args.output,
                java_version=java11,
                type="method_level_select",
                options=["-Dmaven.test.failure.ignore=true", "-Dstandalone=true", "-Dlogging=FINER", "-e", di_cmd, options],
                java11_special=True
            ),
            JUnitResultsToDBHook(
                repository=repository,
                connection=connection,
                report_name="DIRTS_method_level_standalone"
            ),

            # dirts class level
            DIRTSMavenHook(
                repository=repository,
                git_client=git_client,
                report_name="DIRTS_class_level",
                connection=connection,
                output_path=args.output,
                java_version=java11,
                type="class_level_select",
                options=["-Dmaven.test.failure.ignore=true", "-DoverrideExtension=true", "-Dlogging=FINER", "-e", di_cmd, options],
                java11_special=True
            ),
            JUnitResultsToDBHook(
                repository=repository,
                connection=connection,
                report_name="DIRTS_class_level"
            ),

            # dirts method level
            DIRTSMavenHook(
                repository=repository,
                git_client=git_client,
                report_name="DIRTS_method_level",
                connection=connection,
                output_path=args.output,
                java_version=java11,
                type="method_level_select",
                options=["-Dmaven.test.failure.ignore=true", "-DoverrideExtension=true", "-Dlogging=FINER", "-e", di_cmd, options],
                java11_special=True
            ),
            JUnitResultsToDBHook(
                repository=repository,
                connection=connection,
                report_name="DIRTS_method_level"
            ),
        ],
    )
    # create walker

    # start walking
    walker.walk()

    # cleanup
    if tmp_path is not None:
        shutil.rmtree(tmp_path)


if __name__ == "__main__":
    # if this script is called via `python cli.py` run main routine
    main()
