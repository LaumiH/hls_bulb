import glob, os, re
from itertools import islice
import statistics as stats
import matplotlib.pyplot as plot

schedule_lengths = open("schedule_lengths.csv", "w")
schedule_lengths.write(";Resource;Length;Nodes;Unequal\n")
schedule_lengths.close()
schedule_lengths = open("schedule_lengths.csv", "a")

open("no_schedule.txt", "w").close()
no_schedule = open("no_schedule.txt", "a")

unequal = []
no_list_schedule = []
exec_times = dict()
convergence = dict()

faster = dict()
list_scheduler_faster = 0
lazy_alap_faster = 0

root = "./benchmark"
#root = "./benchmark_fastest_30_sec_17_01"

with open(root + "/metrics.txt", "r+") as metrics:
    for line in metrics:
        if "unequal schedules" in line and "tried" not in line:
            dashes = [_.start() for _ in re.finditer("/", line)]
            dfgName = line[dashes[0]+1:dashes[1]]
            unequal.append(dfgName)
        elif line.startswith("Execution times"):
            for time in islice(metrics, 6):
                key = time[time.find(":")+2:time.find(",")]
                time = time[time.find(",")+1:]
                value = time[time.find(":")+2:time.rfind(":")]
                time = time[time.rfind(":")+2:time.rfind(",")]
                d = {value: time.replace("\n", "")}
                if key in exec_times:
                    d_old = exec_times[key]
                    exec_times[key] = d | d_old
                else:
                    exec_times[key] = d
        elif line.startswith("Convergence times"):
            for time in islice(metrics, 6):
                #remove -1, as those did not converge
                time = time.replace(", -1", "")
                key = time[time.find(":")+2:time.find(",")]
                time = time[time.find(",")+1:]
                value = time[time.find(":")+2:time.rfind(":")]
                time = time[time.rfind(":")+2:time.rfind(",")]
                d = {value: time.replace("\n", "")}
                if key in convergence:
                    d_old = convergence[key]
                    convergence[key] = d | d_old
                else:
                    convergence[key] = d

        else:
            for type in ["ASAP", "OWN", "PAPER"]:
                if line.startswith(" lBoundEstimator: " + type) and "than lBoundEstimator: " + type not in line and (line.count("lazy")>1 or line.count("listSchedule")>1):
                    times = line[line.find("times")-4:line.find("times")-1]
                    #times = [int(s) for s in times.split() if s.isdigit()][0]
                    if type in faster:
                        faster[type] = faster[type] + int(times)
                    else:
                        faster[type] = int(times)
                elif line.startswith(" lBoundEstimator: " + type) and "than lBoundEstimator: " + type in line:
                    alap_bound = line[line.find("ALAPBound: ")+11:line.find("faster")-1]
                    times = line[line.find("times")-4:line.find("times")-1]
                    times = [int(s) for s in times.split() if s.isdigit()][0]
                    if alap_bound == "listScheduler":
                        list_scheduler_faster += times
                    elif alap_bound == "lazyALAP":
                        lazy_alap_faster += times

print("Lazy ALAP faster: " + str(lazy_alap_faster))
print("ListScheduler faster: " + str(list_scheduler_faster))

plot.figure(0)
plot.bar(["ASAP faster", "OWN faster", "PAPER faster"], faster.values())
plot.ylabel("Occurances")
plot.savefig('estimator_faster.png')

#plot.show()

x = []
y = []
y_mean = []
y_median = []

x_6 = [1,2,3,4,5,6]

i = 1
for estimator in exec_times:
    for alap in exec_times[estimator]:
        mylist = [int(x) for x in exec_times[estimator][alap].split(',')]
        y_mean.append(stats.mean(mylist))
        y_median.append(stats.median(mylist))
        x += [i]*len(mylist)
        #randomNrs = numpy.random.choice(10, len(mylist), replace=True)
        y += mylist
        i += 1

plot.figure(1)
plot.scatter(x, y)
labels = ["ASAP\nlistSchedule", "ASAP\nlazyALAP", "OWN\nlistSchedule", "OWN\nlazyALAP", "PAPER\nlistSchedule", "PAPER\nlazyALAP"]
plot.xticks([1,2,3,4,5,6], labels)
plot.ylabel("Execution time in ms")
plot.savefig('execution_time.png')
#plot.close()

plot.figure(2)
plot.scatter(x_6, y_mean)
labels = ["ASAP\nlistSchedule", "ASAP\nlazyALAP", "OWN\nlistSchedule", "OWN\nlazyALAP", "PAPER\nlistSchedule", "PAPER\nlazyALAP"]
plot.xticks([1,2,3,4,5,6], labels)
plot.ylabel("Mean execution time in ms")
plot.savefig('mean_execution_time.png')
#plot.close()

plot.figure(3)
plot.scatter(x_6, y_median)
labels = ["ASAP\nlistSchedule", "ASAP\nlazyALAP", "OWN\nlistSchedule", "OWN\nlazyALAP", "PAPER\nlistSchedule", "PAPER\nlazyALAP"]
plot.xticks([1,2,3,4,5,6], labels)
plot.ylabel("Median of execution time in ms")
plot.savefig('median_execution_time.png')
#plot.close()


x = []
y = []
y_mean = []
y_median = []

i = 1
for estimator in convergence:
    for alap in convergence[estimator]:
        mylist = [int(x) for x in convergence[estimator][alap].split(',')]
        y_mean.append(stats.mean(mylist))
        y_median.append(stats.median(mylist))
        x += [i]*len(mylist)
        y += mylist
        i += 1

plot.figure(4)
plot.scatter(x, y)
labels = ["ASAP\nlistSchedule", "ASAP\nlazyALAP", "OWN\nlistSchedule", "OWN\nlazyALAP", "PAPER\nlistSchedule", "PAPER\nlazyALAP"]
plot.xticks([1,2,3,4,5,6], labels)
plot.ylabel("Convergence time in ms")
plot.savefig('convergence_time.png')
#plot.close()

plot.figure(5)
plot.scatter(x_6, y_mean)
labels = ["ASAP\nlistSchedule", "ASAP\nlazyALAP", "OWN\nlistSchedule", "OWN\nlazyALAP", "PAPER\nlistSchedule", "PAPER\nlazyALAP"]
plot.xticks([1,2,3,4,5,6], labels)
plot.ylabel("Mean convergence time in ms")
plot.savefig('mean_convergence_time.png')
#plot.close()

plot.figure(6)
plot.scatter(x_6, y_median)
labels = ["ASAP\nlistSchedule", "ASAP\nlazyALAP", "OWN\nlistSchedule", "OWN\nlazyALAP", "PAPER\nlistSchedule", "PAPER\nlazyALAP"]
plot.xticks([1,2,3,4,5,6], labels)
plot.ylabel("Median of convergence time in ms")
plot.savefig('median_convergence_time.png')
#plot.close()

#plot.show()

too_conservative = dict()
diff_best_initial = []

faster = dict()

own_faster = []
paper_faster = []

timeout = dict()

for filename in glob.iglob(root + '/**', recursive=True):
    if os.path.isfile(filename): # filter dirs
        if "metrics" in filename:
            continue

        dashes = [_.start() for _ in re.finditer("/", filename)]
        dfgName = filename[dashes[1]+1:dashes[2]]
        resName = filename[dashes[2]+1:dashes[3]]

        min_len = 0
        max_len = 0
        nodes = 0

        if "eval" in filename:
            last_line = ""
            with open(filename, "r+") as file:
                for line in file:
                    if line.startswith("Found schedule of length"):
                        length = [int(i) for i in line.split() if i.isdigit()]
                        if dfgName in unequal:
                            if min_len == 0: min_len = length[0]
                            min_len = min(min_len, length[0])
                            max_len = max(max_len, length[0])
                        else:
                            min_len = length[0]
                            max_len = min_len
                        nodes = length[1]

                    elif "list scheduler did not complete once" in line:
                        preamble = open(filename.replace("eval.txt", "preamble.txt"), "r")
                        for line in preamble:
                            if line.startswith("Found schedule of length"):
                                length = [int(i) for i in line.split() if i.isdigit()]
                                nodes = length[1]
                                break
                        no_schedule.write(dfgName + " with " + resName + ", " + str(nodes) + " nodes\n")
                        no_list_schedule.append(dfgName)

                    elif line.startswith("Printing BULB metrics for"):
                        best_latency = 0
                        initial_latency = 0
                        for part in islice(file, 5):
                            if part.startswith("Best latency"):
                                best_latency = int(part[part.find(":")+1:])
                            elif part.startswith("Initial best latency"):
                                initial_latency = int(part[part.find(":")+1:])
                        diff_best_initial.append(initial_latency-best_latency)

                    elif line.startswith("faster: lBoundEstimator: "):
                        if line.startswith("faster: lBoundEstimator: OWN") and "vs. lBoundEstimator: PAPER" in line and (line.count("lazy")>1 or line.count("listSchedule")>1): 
                            #compare PAPER and OWN lower bound estimator
                            alap_bound = line[line.find("ALAPBound: ")+11:line.find("->")-1]
                            difference1 = int(line[line.find("->")+3:line.find(", vs")])
                            difference2 = int(line[line.rfind("->")+3:])
                            difference = difference2
                            if difference1 > 0:
                                difference = difference2 / difference1
                            #if difference > 3:
                                #print(filename + ": OWN with " + alap_bound + " was faster > 3 times")
                            if "--->" not in last_line:
                                print("OWN vs: PAPER: compared schedules are not equal\n")
                            else:
                                own_faster.append(difference)

                        elif line.startswith("faster: lBoundEstimator: PAPER") and "vs. lBoundEstimator: OWN" in line and (line.count("lazy")>1 or line.count("listSchedule")>1): 
                            #compare PAPER and OWN lower bound estimator
                            alap_bound = line[line.find("ALAPBound: ")+11:line.find("->")-1]
                            difference1 = int(line[line.find("->")+3:line.find(", vs")])
                            difference2 = int(line[line.rfind("->")+3:])
                            difference = difference2
                            if difference1 > 0:
                                difference = difference2 / difference1
                            #if difference > 3:
                                #print(filename + ": PAPER with " + alap_bound + " was faster > 3 times")
                            if "--->" not in last_line:
                                print("PAPER vs: OWN: compared schedules are not equal")
                            else:
                                paper_faster.append(difference)

                        
                        for type in ["ASAP", "OWN", "PAPER"]:
                            if line.startswith("faster: lBoundEstimator: " + type) and "vs. lBoundEstimator: " + type not in line and (line.count("lazy")>1 or line.count("listSchedule")>1): 
                                #compare lower bound estimators with same alap upper bound only
                                difference1 = int(line[line.find("->")+3:line.find(", vs")])
                                difference2 = int(line[line.rfind("->")+3:])
                                
                                difference = difference2
                                if difference1 > 0:
                                    difference = difference2 / difference1

                                if "--->" not in last_line:
                                        #print("compared schedules are not equal")
                                        continue

                                #if difference > 1000:
                                    #print(filename + ": " + type + " was faster > 1000 times")

                                if resName not in faster:
                                    faster[resName] = {type:[difference]}
                                elif type not in faster[resName]:
                                    faster[resName] = faster[resName] | {type:[difference]}
                                else:
                                    faster[resName][type].append(difference)

                    last_line = line

            no_schedule.flush()
                    
            if dfgName in unequal:
                schedule_lengths.write(dfgName + ";" + resName + ";" + str(min_len) + ";" + str(nodes) + ";yes\n")
            else:
                schedule_lengths.write(dfgName + ";" + resName + ";" + str(min_len) + ";" + str(nodes) + ";\n")
            schedule_lengths.flush()

        elif "logs" in filename:
            #print("printing timeout before" + str(timeout))
            parameter_line = ""
            with open(filename, "r+") as file:
                for line in file:
                    if "STARTING BULB SCHEDULE with" in line:
                        parameter_line = line
                    elif "Timeout reached while computing" in line:
                        estimator = parameter_line[parameter_line.find("with")+5:parameter_line.find("estimator")-1]
                        bound = parameter_line[parameter_line.find(", ")+2:parameter_line.find("alap ")-1]
                        if estimator in timeout:
                            if bound in timeout[estimator]:
                                timeout[estimator][bound] = timeout[estimator][bound] + 1
                            else:
                                timeout[estimator][bound] = 1
                        else:
                            timeout[estimator] = {bound: 1}


print("\nPrinting timeout: " + str(timeout))

plot.figure(7)
plot.hist(diff_best_initial)
plot.xlabel("Difference between initial and best schedule")
plot.ylabel("Occurances")
plot.savefig('initial_best_diff.png')
#plot.close()

#print(own_faster + paper_faster)

plot.figure(8)
plot.scatter(["OWN"]*len(own_faster) + ["PAPER"]*len(paper_faster), own_faster + paper_faster)
plot.ylabel("Occurances")
plot.savefig('own_vs_paper.png')

plot.figure(9)
plot.bar(["OWN", "PAPER"], [len(own_faster), len(paper_faster)])
plot.xlabel("Times faster than the other")
plot.ylabel("Occurances")
plot.savefig('own_vs_paper_times.png')
print("\n\n\nMax. times faster OWN - PAPER: " + str(max(own_faster)))
print("\n\n\nMax. times faster PAPER - OWN: " + str(max(paper_faster)))


print("Percentage of best schedules at initial run: " + str(diff_best_initial.count(0)/len(diff_best_initial)*100))

x = []
y = []
labels = []
means = []
i = 1
for res in faster:
    for type in faster[res]:
        means.append(stats.mean(faster[res][type]))
        labels += [res + "\n" + type]
        x += [i]*len(faster[res][type])
        y += faster[res][type]
        i += 1

plot.figure(10, figsize=(20,6), dpi=80)
plot.scatter(x, y)
plot.xticks([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18], labels)
plot.xlabel("Slower - faster for res " + res + " and type " + type)
plot.ylabel("Faster ... times")
plot.savefig('slower_faster_diff.png')
##plot.close()

plot.figure(11, figsize=(20,6), dpi=80)
plot.bar(labels, means)
plot.xlabel("Slower - faster for res " + res + " and type " + type)
plot.ylabel("Faster ... times")
plot.savefig('mean_slower_faster_diff.png')
##plot.close()

#plot.plot(stats.mean(diff_best_initial))
#plot.ylabel("Convergence time in s")
#plot.savefig('convergence_time.png')
plot.show()

schedule_lengths.close()
no_schedule.close()
