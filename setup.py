# -*- coding: utf-8 -*-

try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup

readme = ''

setup(
    long_description=readme,
    name='coop',
    version='0.1.65',
    python_requires='==3.*,>=3.6.5',
    author='Daniel Elsner',
    author_email='daniel.elsner@tum.de',
    entry_points={"console_scripts": ["coop = coop.cli.cli:entry_point"]},
    packages=['coop',
              'coop.cli',
              'coop.cli.db',
              'coop.db',
              'coop.evaluation',
              'coop.evaluation.walk',
              'coop.evaluation.walk.impl',
              'coop.evaluation.walk.impl.hooks',
              'coop.models',
              'coop.models.scm',
              'coop.models.scm.impl',
              'coop.models.testing',
              'coop.models.testing.impl',
              'coop.util',
              'coop.util.logging',
              'coop.util.os',
              'coop.util.scm',
              ],
    package_dir={"": "."},
    package_data={},
    install_requires=['click==7.*,>=7.1.2',
                      'gitpython==3.*,>=3.1.3',
                      'halo==0.*,>=0.0.29',
                      'junitparser==2.*,>==2.8.0',
                      'sqlalchemy==1.*,>=1.3.17',
                      'SQLAlchemy-Enum-List==0.*,>=0.1.1',
                      'psycopg2-binary==2.*,>=2.8.6'
                      ],
)
