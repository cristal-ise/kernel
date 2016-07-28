CRISTAL-iSE kernel [![Build Status](https://travis-ci.org/cristal-ise/kernel.svg?branch=master)](https://travis-ci.org/cristal-ise/kernel)[![Javadocs](http://javadoc.io/badge/org.cristalise/cristalise-kernel.svg)](http://javadoc.io/doc/org.cristalise/cristalise-kernel)
==================

[![Join the chat at https://gitter.im/cristal-ise/kernel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/cristal-ise/kernel?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

The core java library of CRISTAL, which provides client and server APIs.

CRISTAL is a description-driven software platform originally developed to track the construction of the CMS ECAL detector of the
LHC at CERN. It consists of a core library, known as the kernel, which manages business objects called Items. Items are entirely 
configured from data, called descriptions, held in other Items. Every change of a state in an Item is a consequence of an 
execution of an activity in that Item's lifecycle, meaning that CRISTAL applications are completely traceable, even in their 
design. It also supports extensive versioning of Item description data, giving the system a high level of flexibility.
