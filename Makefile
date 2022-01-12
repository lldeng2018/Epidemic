# Makefile
# Author: Douglas W. Jones
# Version: Apr. 19, 2021

# Support for:
#   make                    -- make the default target
#   make Epidemic.class     -- the default target

# Plus the following utilities
#   make demo               -- demonstrate the epidemic simulator
#   make clean              -- delete all files created by make
#   make html               -- make javadoc web site from simulator code
#   make shar               -- make shell archive from this directory

########
# named categories of files used below

# model files
ModSrc = PlaceKind.java  Place.java  Role.java  Person.java  InfectionRule.java
ModCls = PlaceKind.class Place.class Role.class Person.class InfectionRule.class

# model support files
ModSupSrc = Time.java  Schedule.java
ModSupCls  = Time.class Schedule.class

# simulation utility files
SimUtilSrc = MyRandom.java  Simulator.java
SimUtilCls  = MyRandom.class Simulator.class

# Input utility files
InpUtilSrc = Error.java  MyScanner.java  Check.java
InpUtilCls  = Error.class MyScanner.class Check.class

# All source files
SimulatorSrc = Epidemic.java $(ModSupSrc) $(ModSrc) $(InpUtilSrc) $(SimUtilSrc)

# Test/demonstration files
Tests = testa testb testc testd

########
# default make for the epidemic simulator

Epidemic.class: Epidemic.java
Epidemic.class: $(InpUtilCls)
Epidemic.class: Simulator.class
Epidemic.class: Time.class
Epidemic.class: PlaceKind.class Role.class Person.class InfectionRule.class
	javac Epidemic.java

########
# core classes of the model used for the epidemic simulator
# Note: there is a dependency knot tying all of the below together, not shown!

PlaceKind.class: PlaceKind.java
PlaceKind.class: $(InpUtilCls)
PlaceKind.class: MyRandom.class
PlaceKind.class: Schedule.class Time.class
PlaceKind.class: Place.class Person.class
	javac PlaceKind.java

Place.class: Place.java
Place.class: PlaceKind.class Person.class
	javac Place.java

Role.class: Role.java
Role.class: $(InpUtilCls)
Role.class: MyRandom.class
Role.class: PlaceKind.class Person.class
Role.class: Schedule.class
	javac Role.java

Person.class: Person.java
Person.class: $(SimUtilCls)
Person.class: Schedule.class Time.class
Person.class: Place.class Role.class InfectionRule.class
	javac Person.java

Schedule.class: Schedule.java
Schedule.class: $(InpUtilCls)
Schedule.class: $(SimUtilCls)
Schedule.class: Time.class
Schedule.class: Place.class Person.class
	javac Schedule.java

########
# support classes of the model used for the epidemic simulator

InfectionRule.class: InfectionRule.java
InfectionRule.class: $(InpUtilCls)
InfectionRule.class: MyRandom.class
InfectionRule.class: Time.class
	javac InfectionRule.java

Time.class: Time.java
	javac Time.java

########
# generic simulation support classes

MyRandom.class: MyRandom.java
	javac MyRandom.java

Simulator.class: Simulator.java
	javac Simulator.java

########
# input management support classes

Check.class: Check.java
Check.class: MyScanner.class Error.class
	javac Check.java

MyScanner.class: MyScanner.java
MyScanner.class: Error.class
	javac MyScanner.java

Error.class: Error.java
	javac Error.java

########
# utility make commands

demo: Epidemic.class
	java Epidemic testa

clean:
	rm -f *.class
	rm -f *.html
	rm -f *.css
	rm -f *.js
	rm -f package-list

html: $(SimulatorSrc)
	javadoc $(SimulatorSrc)

shar: README $(SimulatorSrc) Makefile $(Tests)
	shar README $(SimulatorSrc) Makefile $(Tests) > shar
