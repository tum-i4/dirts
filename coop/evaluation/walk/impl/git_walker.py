# Copyright (c) 2019-present. The coop authors.
import logging
import random
from typing import Optional, List, Union

from git import Repo

from ..base import Walker, Hook
from ....db.scm import DBCommit
from ....models.scm.base import Repository, Commit
from ....models.scm.impl.git import GitClient
from ....util.logging.logger import get_logger

_LOGGER = get_logger(__name__)


class GitWalker(Walker):
    """
       GitWalker class to replay git repository history.
       """

    def __init__(
            self,
            repository: Repository,
            commit_list: Optional[List[str]],
            connection,
            include_merge_commits: bool = False,
            randomize = False,
            branch: Optional[str] = "master",
            num_commits: Optional[int] = 10,
            end_commit: Optional[Commit] = None,
            pre_hooks: Optional[List[Hook]] = None,
            hooks: Optional[List[Hook]] = None,
            post_hooks: Optional[List[Hook]] = None,
            search_terms: Optional[list[str]] = None
    ) -> None:
        super().__init__(
            repository,
            commit_list,
            include_merge_commits,
            branch,
            num_commits,
            end_commit,
            pre_hooks,
            hooks,
            post_hooks,
        )
        self.git_repo: Repo = Repo(repository.path)
        self.git_client: GitClient = GitClient(repository=repository)
        self.search_terms = search_terms
        self.connection = connection
        self.randomize = randomize

    def walk(self) -> None:
        # clean for convenience
        self.git_client.reset_repo(rm_dirs=True)

        if self.commit_list is None:
            self.start_commit = self.git_repo.git.rev_list(self.branch, max_parents=0).splitlines()[0]

            # checkout start commit
            _LOGGER.debug("Checking out commit {}.".format(self.start_commit))
            if isinstance(self.start_commit, Commit):
                self.git_repo.git.checkout(self.start_commit.commit_str)
            else:
                self.git_repo.git.checkout(self.start_commit)

            start_commit = self.git_client.get_commit_from_repo(search_terms=self.search_terms)
            # run pre-hooks
            for h in self.pre_hooks:
                h.run(start_commit)

            # clean partly (keeping cache dirs)
            self.git_client.reset_repo(rm_dirs=False)

            # init counter
            counter = 0

            # walk history along branch
            _LOGGER.debug(
                "Obtaining linear commit history for branch {}.".format(self.branch)
            )

            commits = [commit.hexsha for commit in self.git_repo.iter_commits(
                    "{}..{}".format(start_commit.commit_str, self.branch),
                    ancestry_path=True,
                    no_merges=(not self.include_merge_commits))]

            if self.randomize:
                random.seed(42)
                random.shuffle(commits)
        else:
            commits = self.commit_list

        for commit in commits:
            # get next commit with changeset
            next_commit = self.git_client.get_commit_from_repo(commit_id=commit, search_terms=self.search_terms)
            next_commit.relevant = any(item.relevant for item in next_commit.changelist.items)

            # write commit to DB
            session = self.connection.create_session()
            DBCommit.create_or_update(commit=next_commit, session=session)
            session.commit()

            # continue if we have tokens to search for and this commit is not affected by DI
            if self.search_terms and not any(item.relevant for item in next_commit.changelist.items):
                continue

            _LOGGER.debug("Checking out commit {}.".format(next_commit.commit_str))
            self.git_repo.git.checkout(next_commit.commit_str)

            # run hooks
            success = True
            for h in self.hooks:
                run_successful =  h.run(next_commit)
                success = success and run_successful

            # reset changes and clean untracked files
            # (keep dirs, as they might be required for caching)
            self.git_client.reset_repo(rm_dirs=False)

            # inc counter and break if `num_commits` reached
            if success:
                counter += 1
            if counter >= self.num_commits:
                break

        final_commit = self.git_client.get_commit_from_repo()
        # run post-hooks
        for h in self.post_hooks:
            h.run(final_commit)
