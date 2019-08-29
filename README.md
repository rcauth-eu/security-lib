# NCSA's security-lib

This is an adapted version of [NCSA's security-lib](https://github.com/ncsa/security-lib)
adding among others a `/getproxy` endpoint.

It is a requirement for the also adapted [OA4MP](https://github.com/rcauth-eu/OA4MP).

Both are requirements for the RCauth.eu codebase, including the RCauth
[Delegation Server](https://github.com/rcauth-eu/aarc-delegation-server),
[MasterPortal](https://github.com/rcauth-eu/aarc-master-portal),
[demo VO-portal](https://github.com/rcauth-eu/aarc-vo-portal) and
[SSH Key portal](https://github.com/rcauth-eu/aarc-ssh-portal).

## Prerequisites

* Java 8+ (OpenJDK 8 and 10 are both supported for building)
* [Maven](https://maven.apache.org/) 3.5+

## Compiling and installing

1. Check out the right RCauth-based branch, see the different RCauth components for the required versions.  
   For example:

        git checkout v4.2-RCauth-1

   *Make sure to use the same branch or tag for the OA4MP and security-lib components !!*

2. Compile and install the security-lib

        mvn clean package install

## Background and further reading

https://wiki.nikhef.nl/grid/AARC_Pilot
