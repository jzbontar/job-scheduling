/* Copyright 2011 Jure Žbontar <jure.zbontar@gmail.com>
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package si.zbontar;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;


class Skill {
	int id;
	int quantityDaily;
	float costDaily;
	User user;
	
	public Skill(int id, User user) {
		this.id = id;
		this.user = user;
	}
}


class User {
	int id;
	Map<Integer, Skill> skills = new HashMap<Integer, Skill>();
	
	/* time of last completed task */
	double lastTask;
	
	/* list of late tasks a user can perform */
	List<Task> tasks = new LinkedList<Task>(); 
	
	public User(int id) {
		this.id = id;
	}
}


class Task {
	int id;
	int skill_id;
	List<Skill> skills;
	int quantity;
	float value;
	float due;
	
	/* is the value finish dirty? */
	boolean dirty = true;
	
	/* the completion time of this task if you spill it */
	double finish;
	
	/* the cheapest way to complete this task is to schedule it to cheapSkill */
	Skill cheapSkill;
	
	public Task(int id) {
		this.id = id;
	}
}


class Score {
	/* The score is composed of two parts: delay and profit */
	int delay;
	double profit;
	
	public Score(int delay, double profit) {
		this.delay = delay;
		this.profit = profit;
	}
}


public class Scheduler {
	StringBuilder plan;
	User[] users;
	List<Skill>[] skills;
	Task[] tasks;
	
	LinkedList<Task> late; /* tasks that will not finish on time */
	TreeSet<Task> ontime; /* tasks that have a chance of finishing on time */
	
	public static String run(String conf) {
		try {
			return (new Scheduler(new BufferedReader(new StringReader(conf))).plan.toString());
		} catch (Exception e) {
			return "";
		}
	}
	
	/* Parse the input and store it in arrays users, skills and tasks.
	 * 
	 * The official parsing function was slow. Replacing the Scanner class 
	 * with BufferedReader makes the method run about 50 times faster. 
	 */		
	@SuppressWarnings("unchecked")
	public void parse(BufferedReader in) throws Exception {
		int num_users = Integer.parseInt((new StringTokenizer(in.readLine())).nextToken());
		users = new User[num_users];
		for (int i = 0; i < users.length; i++)
			users[i] = new User(i);
			
		int num_skills = Integer.parseInt((new StringTokenizer(in.readLine())).nextToken());
		skills = new ArrayList[num_skills];
		for (int i = 0; i < skills.length; i++)
			skills[i] = new ArrayList<Skill>();
		
		for (;;) {
			String line = in.readLine();
			if (line.equals(""))
				break;
			StringTokenizer st = new StringTokenizer(line);
			User user = users[Integer.parseInt(st.nextToken())];
			int skill_id = Integer.parseInt(st.nextToken());
			Skill skill = new Skill(skill_id, user);			
			skill.quantityDaily = Integer.parseInt(st.nextToken());
			skill.costDaily = Float.parseFloat(st.nextToken());
			skills[skill_id].add(skill);			
			user.skills.put(skill_id, skill);
		}
		
		int num_tasks = Integer.parseInt((new StringTokenizer(in.readLine())).nextToken());
		tasks = new Task[num_tasks];
				
		for (;;) {
			String line = in.readLine();
			if (line == null)
				break;
			StringTokenizer st = new StringTokenizer(line);
			int task_id = Integer.parseInt(st.nextToken());
			Task task = new Task(task_id);
			task.skill_id = Integer.parseInt(st.nextToken());
			task.skills = skills[task.skill_id];
			task.quantity = Integer.parseInt(st.nextToken());
			task.value = Float.parseFloat(st.nextToken());
			task.due = Float.parseFloat(st.nextToken());
			tasks[task_id] = task;		
		}
	}
	
	/* Calculate the score of the solution stored in the static variable plan */  
	public Score score() {
		StringTokenizer st = new StringTokenizer(plan.toString());

		int[] taskQuantity = new int[tasks.length];
		double[] taskTimeFinish = new double[tasks.length];
		double[] userTimeFinish = new double[users.length];
		double profit = 0;

		while (st.hasMoreTokens()) {
			User user = users[Integer.parseInt(st.nextToken())];
			Task task = tasks[Integer.parseInt(st.nextToken())];
			Skill userSkill = user.skills.get(task.skill_id);
			int quantityPerTask = Integer.parseInt(st.nextToken());
			taskQuantity[task.id] += quantityPerTask;
			userTimeFinish[user.id] += (double)quantityPerTask / userSkill.quantityDaily;
			taskTimeFinish[task.id] = Math.max(taskTimeFinish[task.id], userTimeFinish[user.id]);
			profit -= userSkill.costDaily * (double)quantityPerTask / userSkill.quantityDaily;
		}

		double delay = 0.0;    	
		for (int i = 0; i < tasks.length; i++) {
			Task t = tasks[i];
			delay += Math.max(0, taskTimeFinish[i] - t.due);
			profit += t.value;
		}			    	

		return new Score((int)(delay / tasks.length * 1000 + 0.01), 1000 / profit);
	}
	
	/* Store the assignment of task to skill into the static variable plan */
	public void assign(Skill skill, Task task, int quantity) {
		skill.user.lastTask += (double)quantity / skill.quantityDaily;
		plan.append(skill.user.id + " " + task.id + " " + quantity + "\n");
	}
	
	/* Assign the task to all available users in parallel. If assign is 
	 * False just return the completion time. 
	 */
	public double spillTask(Task task, boolean assign) {
		User sentinelUser = new User(-1);
		sentinelUser.lastTask = Double.POSITIVE_INFINITY;
		Skill sentinelSkill = new Skill(-1, sentinelUser);	
		
		Skill[] skills = new Skill[task.skills.size() + 1];
		int i = 0;
		for (Skill skill2 : task.skills)
			skills[i++] = skill2;
		skills[skills.length - 1] = sentinelSkill;			

		Arrays.sort(skills, new Comparator<Skill>()	{ 
			public int compare(Skill s1, Skill s2) {
				return (int)Math.signum(s1.user.lastTask - s2.user.lastTask);
			}
				});			

		long rate = 0;
		double newQuantity = task.quantity;
		double quantity = 0;		
		for (i = 0; newQuantity > 0; i++) {
			quantity = newQuantity;
			rate += skills[i].quantityDaily;
			newQuantity = quantity - (skills[i + 1].user.lastTask - skills[i].user.lastTask) * rate;
		}
		i--;			
		
		double finish = quantity / rate + skills[i].user.lastTask;
		if (assign) {
			int qSum = 0;		
			for (int j = 0; j < i; j++) {
				int q = (int)(skills[j].quantityDaily * (finish - skills[j].user.lastTask) + 0.5);
				qSum += q;
				assign(skills[j], task, q);
			}			
			assign(skills[i], task, Math.max(0, task.quantity - qSum));
		}
		return finish;
	}
	
	/* The scheduling algorithm */
	public Scheduler(BufferedReader config) throws Exception {	
		parse(config);
		
		/* initialization */
		for (Task task : tasks) {
			double minCost = Double.POSITIVE_INFINITY;
			task.cheapSkill = null;
			
			for (Skill skill : task.skills) {
				double newCost = skill.costDaily * (double)task.quantity / skill.quantityDaily;
				if (newCost < minCost) {
					minCost = newCost;
					task.cheapSkill = skill;
				}
			}
		}
		
		LinkedList<Task> order = optimizeLateness();
		optimizeProfit(order);
	}
	
	/* Ignore the profit and optimize the average lateness. */
	public LinkedList<Task> optimizeLateness() {
		plan = new StringBuilder();		
		late = new LinkedList<Task>();
		ontime = new TreeSet<Task>(new Comparator<Task>() {
			public int compare(Task t1, Task t2) {
				if (t1.due < t2.due)
					return -1;
				if (t1.due > t2.due)
					return 1;
				return t1.id - t2.id;
			}			
		});
		
		for (Task task : tasks) {
			if (task.due <= 0)
				addLate(task);
			else
				ontime.add(task);
		}
		
		LinkedList<Task> order = new LinkedList<Task>();
		while (!(late.isEmpty() && ontime.isEmpty())) {
			if (!late.isEmpty()) {
				/* We have a late task. Select the one with the earliest finish time and spill it. */				
				Task minTask = null;
				double minFinish = Double.POSITIVE_INFINITY;
				for (Task task : late) {				
					if (task.dirty) {
						task.finish = spillTask(task, false);
						task.dirty = false;
					}
						
					if (task.finish < minFinish) {
						minTask = task;
						minFinish = task.finish;
					}
				}
				removeLate(minTask);
				spillTask(minTask, true);
				order.add(minTask);

				double maxLastTask = Double.NEGATIVE_INFINITY;
				for (User user : users)
					maxLastTask = Math.max(maxLastTask, user.lastTask);
				
				/* Check for tasks that were on time but could now be late.  */
				LinkedList<Task> newLate = new LinkedList<Task>();
				for (Task task : ontime) {
					double lastTask = Double.POSITIVE_INFINITY;
					for (Skill skill : task.skills)
						lastTask = Math.min(lastTask, skill.user.lastTask);
					if (task.due <= lastTask)
						newLate.add(task);
					if (task.due > maxLastTask)
						break;
				}
				for (Task task : newLate) {
					ontime.remove(task);
					addLate(task);
				}
				markDirty(minTask);			
			} else {
				/* There are no late tasks. Select the one with the earliest deadline and 
				 * try to assign it to one user. If the task finishes after it's deadline 
				 * don't assign it. Mark it as late instead. 
				 */
				Task task = ontime.pollFirst();
				
				Skill minSkill = null;
				double minFinished = Double.POSITIVE_INFINITY;
				for (Skill skill : task.skills) {
					double finished = skill.user.lastTask + (double)task.quantity / skill.quantityDaily;
					if (finished < minFinished) {
						minSkill = skill;
						minFinished = finished;
					}
				}
				
				if (minFinished > task.due) {
					addLate(task);
				} else {
					assign(minSkill, task, task.quantity);	
					order.add(task);
					markDirty(task);	
				}
			}
		}
		return order;
	}
	
	public void addLate(Task task) {
		late.add(task);
		for (Skill skill : task.skills)
			skill.user.tasks.add(task);
	}
	
	public void removeLate(Task task) {
		late.remove(task);
		for (Skill skill : task.skills)
			skill.user.tasks.remove(task);
	}
	
	public void markDirty(Task changedTask) {
		for (Skill skill : changedTask.skills)
			for (Task task : skill.user.tasks)
				task.dirty = true;
	}
	
	/* Try to find a solution with the same average delay but better profit.
	 * The strategy is to use a simple scheduling algorithm with the order of 
	 * tasks fixed from the previous phase and try to assign some tasks to the 
	 * cheapest worker. Use bisection to find the largest amount of tasks 
	 * that can be assigned this way.
	 */
	public void optimizeProfit(LinkedList<Task> order) {
		StringBuilder bestPlan = plan;
		Score targetScore = score();
		
		double l = 0.0;
		double r = 10.0;
		
		for (int i = 0; i < 20; i++) {
			double m = (l + r) / 2;
			spillProfit(order, m);
			Score score = score();
			if (score.delay == targetScore.delay) {
				l = m;
				if (score.profit < targetScore.profit) {
					targetScore.profit = score.profit;
					bestPlan = plan;
				}
			} else {
				r = m;
			}
		}
		plan = bestPlan;		
	}
	
	/* Simple scheduling algorithm with the order of tasks fixed. For every task 
	 * search for the cheapest user that can complete the task before the 
	 * deadline. If no such user exists spill the task. 
	 */
	public void spillProfit(LinkedList<Task> order, double margin) {
		plan = new StringBuilder();
		for (User user : users)
			user.lastTask = 0;
		
		for (Task task : order) {
			Skill minSkill = null;
			double minCost = Double.POSITIVE_INFINITY;
			for (Skill skill : task.skills) {
				double finished = skill.user.lastTask + (double)task.quantity / skill.quantityDaily;
				double cost = skill.costDaily * (double)task.quantity / skill.quantityDaily;
				if (finished < task.due && cost < minCost) {
					minSkill = skill;
					minCost = cost;
				}
			}
			
			double minCostFinish = task.cheapSkill.user.lastTask + (double)task.quantity / task.cheapSkill.quantityDaily;
			double normalFinish;
			if (minSkill == null)
				normalFinish = spillTask(task, false);
			else
				normalFinish = minSkill.user.lastTask + (double)task.quantity / minSkill.quantityDaily;
			
			if (minCostFinish - normalFinish < margin) {
				assign(task.cheapSkill, task, task.quantity);
			} else if (minSkill == null) {
				spillTask(task, true);
			} else {
				assign(minSkill, task, task.quantity);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		Scheduler s = new Scheduler(new BufferedReader(new InputStreamReader(System.in)));
		System.out.println(s.plan);
	}
}
