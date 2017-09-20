package se.oru.coordination.coordination_oru.tests;

import java.io.File;
import java.util.Comparator;

import org.metacsp.multi.spatioTemporal.paths.Pose;

import com.vividsolutions.jts.geom.Coordinate;

import se.oru.coordination.coordination_oru.ConstantAccelerationForwardModel;
import se.oru.coordination.coordination_oru.CriticalSection;
import se.oru.coordination.coordination_oru.Mission;
import se.oru.coordination.coordination_oru.RobotAtCriticalSection;
import se.oru.coordination.coordination_oru.RobotReport;
import se.oru.coordination.coordination_oru.demo.DemoDescription;
import se.oru.coordination.coordination_oru.motionplanning.ReedsSheppCarPlanner;
import se.oru.coordination.coordination_oru.simulation2D.TrajectoryEnvelopeCoordinatorSimulation;
import se.oru.coordination.coordination_oru.util.Missions;

@DemoDescription(desc = "Example showing that parking poses are considered in coordination.")
public class TestTrajectoryEnvelopeCoordinatorWithMotionPlanner14 {

	public static void main(String[] args) throws InterruptedException {

		double MAX_ACCEL = 1.0;
		double MAX_VEL = 5.0;
		//Instantiate a trajectory envelope coordinator.
		//The TrajectoryEnvelopeCoordinatorSimulation implementation provides
		// -- the factory method getNewTracker() which returns a trajectory envelope tracker
		// -- the getCurrentTimeInMillis() method, which is used by the coordinator to keep time
		//You still need to add one or more comparators to determine robot orderings thru critical sections (comparators are evaluated in the order in which they are added)
		final TrajectoryEnvelopeCoordinatorSimulation tec = new TrajectoryEnvelopeCoordinatorSimulation(MAX_VEL,MAX_ACCEL);
		tec.addComparator(new Comparator<RobotAtCriticalSection> () {
			@Override
			public int compare(RobotAtCriticalSection o1, RobotAtCriticalSection o2) {
				CriticalSection cs = o1.getCriticalSection();
				RobotReport robotReport1 = o1.getTrajectoryEnvelopeTracker().getRobotReport();
				RobotReport robotReport2 = o2.getTrajectoryEnvelopeTracker().getRobotReport();
				return ((cs.getTe1Start()-robotReport1.getPathIndex())-(cs.getTe2Start()-robotReport2.getPathIndex()));
			}
		});
		tec.addComparator(new Comparator<RobotAtCriticalSection> () {
			@Override
			public int compare(RobotAtCriticalSection o1, RobotAtCriticalSection o2) {
				return (o2.getTrajectoryEnvelopeTracker().getTrajectoryEnvelope().getRobotID()-o1.getTrajectoryEnvelopeTracker().getTrajectoryEnvelope().getRobotID());
			}
		});
		
		Coordinate footprint1 = new Coordinate(-0.5,0.5);
		Coordinate footprint2 = new Coordinate(-0.5,-0.5);
		Coordinate footprint3 = new Coordinate(0.7,-0.5);
		Coordinate footprint4 = new Coordinate(0.7,0.5);
		tec.setDefaultFootprint(footprint1, footprint2, footprint3, footprint4);
		
		//You probably also want to provide a non-trivial forward model
		//(the default assumes that robots can always stop)
		tec.setForwardModel(1, new ConstantAccelerationForwardModel(MAX_ACCEL, MAX_VEL, tec.getControlPeriod(), tec.getTemporalResolution()));
		tec.setForwardModel(2, new ConstantAccelerationForwardModel(MAX_ACCEL, MAX_VEL, tec.getControlPeriod(), tec.getTemporalResolution()));

		//Need to setup infrastructure that maintains the representation
		tec.setupSolver(0, 100000000);

		//Setup a simple GUI (null means empty map, otherwise provide yaml file)
		tec.setupGUI(null);

		Pose startRobot1 = new Pose(1.0,1.0,Math.PI/4);
		Pose goalRobot1 = new Pose(10.0,10.0,Math.PI/4);

		Pose startRobot2 = new Pose(19.0,1.0,3*Math.PI/4);
		Pose goalRobot2 = new Pose(10.0,10.0,3*Math.PI/4);
		Pose goalRobot2Next = new Pose(19.0,10.0,Math.PI/2);

		//Place robots in their initial locations (looked up in the data file that was loaded above)
		// -- creates a trajectory envelope for each location, representing the fact that the robot is parked
		// -- each trajectory envelope has a path of one pose (the pose of the location)
		// -- each trajectory envelope is the footprint of the corresponding robot in that pose
		tec.placeRobot(1, startRobot1);
		tec.placeRobot(2, startRobot2);

		String yamlFile = "maps/map-empty.yaml";
		ReedsSheppCarPlanner rsp = new ReedsSheppCarPlanner();
		rsp.setMapFilename("maps"+File.separator+Missions.getProperty("image", yamlFile));
		double res = 0.2;// Double.parseDouble(getProperty("resolution", yamlFile));
		rsp.setMapResolution(res);
		rsp.setRobotRadius(1.1);
		rsp.setTurningRadius(4.0);
		rsp.setDistanceBetweenPathPoints(0.1);
		
		rsp.setStart(startRobot1);
		rsp.setGoals(goalRobot1);
		rsp.plan();
		Missions.putMission(new Mission(1,rsp.getPath()));
		
		rsp.setStart(startRobot2);
		rsp.setGoals(goalRobot2);
		rsp.plan();
		Missions.putMission(new Mission(2,rsp.getPath()));
		Missions.putMission(new Mission(2,rsp.getPathInv()));
		rsp.setStart(startRobot2);
		rsp.setGoals(goalRobot2Next);
		rsp.plan();
		Missions.putMission(new Mission(2,rsp.getPath()));
		
		System.out.println("Added missions " + Missions.getMissions());

		tec.addMissions(Missions.getMission(1, 0), Missions.getMission(2, 0));
		tec.computeCriticalSections();
		tec.startTrackingAddedMissions();
		
		Thread.sleep(20000);

		tec.addMissions(Missions.getMission(2, 1));
		tec.computeCriticalSections();
		tec.startTrackingAddedMissions();

		Thread.sleep(10000);

		tec.addMissions(Missions.getMission(2, 2));
		tec.computeCriticalSections();
		tec.startTrackingAddedMissions();

		

	}

}
