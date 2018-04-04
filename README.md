CRISTAL-iSE kernel [![Build Status](https://travis-ci.org/cristal-ise/kernel.svg?branch=master)](https://travis-ci.org/cristal-ise/kernel)[![Javadocs](http://javadoc.io/badge/org.cristalise/cristalise-kernel.svg)](http://javadoc.io/doc/org.cristalise/cristalise-kernel)
==================

[![Join the chat at https://gitter.im/cristal-ise/kernel](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/cristal-ise/kernel?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

The core java library of CRISTAL, which provides client and server APIs.

CRISTAL is a description-driven software platform developed to track the construction of the CMS ECAL detector of the
LHC at CERN. It consists of a core library(known as the kernel), which manages business objects called Items. Items are  
configured from data, called descriptions, held in other Items. An execution of an activity in Item's lifecycle will change the state of the Item, meaning that CRISTAL applications are completely traceable. It also supports extensive versioning of Item description data, giving the system a high level of flexibility.
