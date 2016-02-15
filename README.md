Algorithm for Optimal Job Scheduling and Task Allocation under Constraints
==========================================================================

This program won first place at the Job Scheduling challenge hosted on
[tunedit](http://tunedit.org/challenge/job-scheduling) in 2011. This document
contains the description of the task and the main ideas of my solution.

Task
----

Please devise an algorithm which assigns tasks to users, in a way that

    Score = AverageLateness + 1000 / Profit

is minimized. AverageLateness is calculated over all tasks, a task
finished on time or earlier has zero lateness. Profit is simply the
total value of tasks minus cost of doing them according to the generated
plan. The score is chosen in such a way that AverageLateness occupies 3
higher digits after comma (is rounded to 3 digits), while 1000 / Profit
typically occupies digits 4 - 6 after comma.

Format of the configuration file:

    N Users
    M Skills
    [X lines of: user id, skill id of this user, quantity user can do daily, this user daily skill usage cost]
    1st_userID 1st_skillID daily_quantity daily_cost
    1st_userID 2nd_skillID daily_quantity daily_cost
    ...
    1st_userID last_skillID daily_quantity daily_cost
    2nd_userID 1st_skillID daily_quantity daily_cost
    ...
    2nd_userID last_skillID daily_quantity daily_cost
    ...
    N-th_userID 1st_skillID daily_quantity daily_cost
    ...
    N-th_userID last_skillID daily_quantity daily_cost
    [empty line]
    K Tasks
    [Y lines of: task_id, skill id required, quantity of task to do, value of task, time in days to do the task]
    1st_taskID skillID quantity value timeleft
    2nd_taskID skillID quantity value timeleft
    ...
    n-th_taskID skillID quantity value timeleft


Daily quantities of users and task are integer numbers. Daily work
costs and values of tasks are 0.1 precision floats. Timelefts are 0.01
precision floats.

Your algorithm must be implemented in Java. As a solution, compiled code
should be submitted in a form of a JAR archive, with a public class of
any name (you must type the class name on Submit form) containing:

    static String run(String conf)

method that implements the algorithm: takes configuration as a String and
outputs the plan as a String. View the Baseline algorithm class for an
example. You are free to use parts of the Baseline code, such as parsing,
in your own algorithm.

The output plan should comprise a number of lines, each one terminated
with "\n". Every line should specify what task will be done next by a
given user and in what quantity. Lines should be formatted as:

    userID taskID quantityToDo

where IDs correspond to IDs in configuration file and quantityToDo must
be an integer >= 1.

This information is enough to reconstruct an exact schedule, including
timings, for all the users and calculate the score. For your own in-house
experiments and scoring of plans, it may be helpful to use the source
code of evaluation procedure.

Preliminary evaluation is executed over 5 different configurations, and
the score is averaged over all of them. Final evaluation will employ 15
different configurations, to guarantee high precision of results.

There is a time limit for evaluation. Your algorithm should spend no
more than 5 minutes per configuration. Memory limit is 1GB. Tests will
be executed on a single-core 2.8 GHz CPU with Debian operating system,
under Sun Java Runtime Environment.


Algorithm Description
---------------------

From the scoring function it is clear that our submissions were compared
by average lateness first and profit second. Hence, I decided to split my
algorithm into two parts. First a heuristic algorithm computes a schedule
optimizing lateness, then the solution is modified so that profit is
maximized but average lateness remains constant. Recall that the scoring
function rounds the average lateness component to 3 digits, allowing us
to modify the solution in the second phase quite a bit before average
lateness starts to increase.

### Optimizing Average Lateness

The order by which tasks are scheduled is determined as follows. From
the set of tasks which have no chance of completing on time the one with
the earliest completion time is selected. If there are no such tasks,
the task with the earliest deadline is selected.

Once a task is selected for scheduling it is assigned to as many users
as possible so as to minimize the completion time. The process is called
spilling a task as it is akin to pouring water into a cup and is depicted
in the illustration below. For details see the method spillTask. 

              Before spilling task 'o'
              ------------------------
    User i_1 |**********
    User i_2 |
    User i_3 |*********************
    User i_4 |***
              ------------------------

              After spilling task 'o'
              ------------------------
    User i_1 |**********oooooo
    User i_2 |oooooooooooooooo
    User i_3 |*********************
    User i_4 |***ooooooooooooo
              ------------------------

Since spilling all tasks would sometimes cause a memory limit on the
output string (if a task is split among n users the output contains n
lines), only the tasks that were late were spilled, the others where
assigned to a single user.

The output of this phase is the average lateness and the order of tasks
that achieved it.

### Optimizing Profit

The idea is to keep the order of tasks from the first stage but instead
of spilling every task assign it to the users that will complete it for
the least amount of money. Do this for all tasks with 
    
    minCostFinish - normalFinish < margin

where minCostFinish is the completion time of a task assigned to the
"cheapest" user and normalFinish is the completion time of a spilled
task. Obviously a bigger value of margin implies a bigger or equal
profit, but a bigger margin can also increase average lateness. The
biggest margin for which the average lateness remained the same as in
the first phase was found with bisection.

Usage
-----

You will first need to compile the program.

	$ javac si/zbontar/Scheduler.java

To run the scheduler on the provided sample file, first gunzip the
configuration file and the correct output:

	$ gunzip sample/in.txt.gz
	$ gunzip sample/out.txt.gz

Then, run the Scheduler:

	$ java si/zbontar/Scheduler < sample/in.txt > out.txt

The output file contains lines with three integers each. The format
is ``userID taskID quantityToDo''.

	$ cat out.txt
	3348 434365 41405
	2094 306026 22271
	6208 180629 50603
	...
	2173 462050 249157
	2387 462467 427405
	3432 462590 81724

Make sure that the computed output file is the same as the correct output file.
The output of the following diff command should be empty.

	$ diff out.txt sample/out.txt
