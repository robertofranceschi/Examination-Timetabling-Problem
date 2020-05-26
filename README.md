# Examination Timetabling Problem
This repository contains the project assignment for the _**Decision Making and Optimization**_ course at the Politecnico di Torino. 

Group 12: Chiara Tomei, Gabriele Tiboni, Paola Privitera, Roberto Fransceschi, Tommaso Calò, Valerio Di Eugenio

## Abstract
This project presents a heuristic to solve a simplified examination timetabling problem. 

## Problem specifications

### Input format
- `instanceXX.exm`: defines the total number of students enrolled per exam. <br>Format: 1 line per exam. Each line has the format:
            INT1 INT2
where `INT1` is the exam ID and `INT2` is the number of enrolled students in exam INT1.

- `instanceXX.slo`: defines the length of the examination period.<br>Format: a single value in the format
            INT
where `INT` is the number of available time-slots (i.e., `t_max`). Hence, time-slots will have numeric IDs `{1, 2, ..., tmax}`.

- `instanceXX.stu`: defines the exams in which each student is enrolled. <br>Format: 1 line for each enrollment. Each line has the format:
            sINT1 INT2
where INT1 is the student ID and INT2 is the ID of the exam in which student INT1 is enrolled.

## Linear Programmimg - Problem Formulation


### Run the .jar file
In order to run the program the input files (`.exm`, `.slo`, `.stu`) for every instance _**must be**_ in the same folder of the `.jar` file.

      $ java -jar ETPsolver.jar instancename -t tlim

You can find the output file (`instanceXX.sol`) again in the same folder of the `.jar` file. The file .sol will be overwritten
every time a better solution is found.

### References

