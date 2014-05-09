#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
   Primary OmeroPy types

   Classes:
      - omero.client    -- Main OmeroPy connector object

   Copyright 2007, 2008 Glencoe Software, Inc. All rights reserved.
   Use is subject to license terms supplied in LICENSE.txt

"""


from omero_version import omero_version
from omero_version import ice_compatibility as compat
import Ice
import os
_sys = __import__("sys")

try:
    vers = Ice.stringVersion()
    vers = vers.split(".")
    compat = compat.split(".")
    if compat[0:2] != vers[0:2]:
        msg = """

        ERROR: Ice version mismatch!

        Your OMERO code has been compiled using Ice version %s
        but you seem to have Ice version %s installed. If you need
        help understanding this issue, please send this error message
        to the OME community:

        http://www.openmicroscopy.org/site/community

        Debugging Info:
        --------------
        VERSION=%s
        PYTHONPATH=%s
        """ % (".".join(compat), ".".join(vers), omero_version,
               os.path.pathsep.join(_sys.path))
        raise Exception(msg)
finally:
    del omero_version
    del compat
    del vers
    del Ice
    del os

__import_style__ = None


def client_wrapper(*args, **kwargs):
    """
    Returns an instance of L{omero.gateway.BlitzGateway} created with all
    arguments passed to this method

    @return:    See above
    """
    import omero.gateway
    return omero.gateway.BlitzGateway(*args, **kwargs)


def client(*args, **kwargs):
    import omero.clients
    return omero.clients.BaseClient(*args, **kwargs)


class ClientError(Exception):
    """
    Top of client exception hierarchy.
    """
    pass


class UnloadedEntityException(ClientError):
    pass


class UnloadedCollectionException(ClientError):
    pass


def proxy_to_instance(proxy_string):
    import omero
    parts = proxy_string.split(":")
    kls = parts[0]
    if not kls.endswith("I"):
        kls += "I"
    kls = getattr(omero.model, kls, None)
    if kls is None:
        raise ClientError(("Invalid proxy string: %s. "
                          "Correct format is Class:ID") % proxy_string)
    return kls(proxy_string)

#
# Workaround for warning messages produced in
# code-generated Ice files.
#
if _sys.version_info[:2] == (2, 6):
    import warnings
    warnings.filterwarnings(
        action='ignore',
        message='BaseException.message has been deprecated as of Python 2.6',
        category=DeprecationWarning)
