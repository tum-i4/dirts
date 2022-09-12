#!/bin/bash

num_commits=${1}

type=${2}
project_dir=$(realpath ${3})
branch=${4}
options=${@:5}

export PYTHONPATH="../"
python3 ../dirts/dirts-di-walker.py "${project_dir}" --${type} -n "${num_commits}" -b "${branch}" --options="${options}"
