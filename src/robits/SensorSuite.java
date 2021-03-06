package robits;

import java.util.Collections;
import java.util.LinkedList;

import action.AOE;
import worldData.Obstruction;
import worldData.World;

public class SensorSuite{
   private int[] hearingSense;   //contains 4 ints from 0 to 100. 
   /*                            //each number represents the level of noise the creature detects in that direction
   index | direction             //detects the presence of all species, friend or foe
     0   |   Up                  //the other creatures (stealth-100)/10 is added to the distance
     1   |   Right               //full range      
     2   |   Down
     3   |   Left
   */
   //NOTE: all smell senses have (smellEfficiencyCoeficent, a var declared in the World class) the effective range of hearing and sight
   private int[] energySmellSense;     //same idea as hearing except that the creature can detect the presence of energy 
                                       //the difference is that activity is muiltiplied by the (ammount of energy)/50. Thus grass will smell a little and whole corpses a lot   
   private int[] allySmellSense;       //same deal as the others. 4 items representing directions. yadda yadda
                                       //only detects members of the same species. Not affected by the stealth stat
   private int[] enemySmellSense;      //only detect other species. affected by the stealth stat of others                                 
   
   //adjacency senses                                    
   private int[] obstructionTouchSense;//4 ints in the same layout as before, except that the ints are the health of the obstruction instead of the distance
   private int[] energyTouchSense;     //has 5 ints, the fifth represents the tile under the creature
   private boolean[] allyTouchSense;   //boolean wether or not there is an ally adjacent to the current position                                    
   private boolean[] enemyTouchSense;  //booleans for detecting enemies by touch
   

   //sight is a complicated sense. it only can look for one thing at a time and will only detect the closest of that thing.
   /*
   for an example. say you want your creature to look for allies. in your mind code use the following
      this.creature.getSensorSuite().setSightTargetType(SightTarget.ALLY));
   
   for every game tick following that command (not including the current one)... 
   creature.SesnorSuite.sightHasTarget will represent if the sight sense has detected anything
   creature.SesnorSuite.sightAngle will represent the angle (in degres, 0-359) to the closest target,
   creature.SesnorSuite.sightSense will be how visible that target is 
   */
   private SightTargetType sightTargetType;   //look at the SightTarget enum. its just the thing you are looking for. (default is enemies)
   private int sightAngle;                    //the angle to the nearest (whatever you are looking for). 0 degrees is straight right, 90 up, 180 left, 270 down 
   private int sightSense;                    //how visible the target is. number from 0 to 100, affected by both distance and (when applicable) stealth stats
   private boolean sightHasTarget;            //whether or not the sight sense has found a target in it's sensory range
   
   private SightTarget sightTarget;
   private boolean nextTargetScheduled;
   private boolean prevTargetScheduled;
   
   private final double smellDistanceModifier;
   private final int baseSensorRange;
   
   
   //-----------------------------------------------------------------constructor
   public SensorSuite(){
      //just init the arrays and call it good. the world injects arrays not values so this is onlt to stop nullPointerExceptions on the first tick(might not even be an issue)
      this.hearingSense = new int[4];
      this.energySmellSense = new int[4];
      this.allySmellSense = new int[4];
      this.enemySmellSense = new int[4];
      this.obstructionTouchSense = new int[4];
      this.energyTouchSense = new int[5];         //the fifth slot is the one directly underneath the creature
      this.allyTouchSense = new boolean[4];
      this.enemyTouchSense = new boolean[4];
      this.sightTargetType = SightTargetType.ROBIT;
      smellDistanceModifier = 0.5;
      baseSensorRange = 10;

   }
   public SensorSuite(int baseSensorRange,double smellDistanceModifier) {
	   this.hearingSense = new int[4];
	   this.energySmellSense = new int[4];
	   this.allySmellSense = new int[4];
	   this.enemySmellSense = new int[4];
	   this.obstructionTouchSense = new int[4];
	   this.energyTouchSense = new int[5];         //the fifth slot is the one directly underneath the creature
	   this.allyTouchSense = new boolean[4];
	   this.enemyTouchSense = new boolean[4];
	   this.sightTargetType = SightTargetType.ROBIT;
	   this.baseSensorRange = baseSensorRange;
	   this.smellDistanceModifier = smellDistanceModifier;
   }
   
   public SensorSuite(int[] hearingSense, int[] energySmellSense, int[] allySmellSense, int[] enemySmellSense, 
                      int[] obstructionTouchSense, int[] energyTouchSense, boolean[] allyTouchSense, boolean[] enemyTouchSense,
                      SightTargetType sightTargetType, int sightAngle, int sightSense, boolean sightHasTarget){
 
      this.hearingSense = hearingSense;
      this.energySmellSense = energySmellSense;
      this.allySmellSense = allySmellSense;
      this.enemySmellSense = enemySmellSense;
      this.obstructionTouchSense = obstructionTouchSense;
      this.energyTouchSense = energyTouchSense;
      this.allyTouchSense = allyTouchSense;
      this.enemyTouchSense = enemyTouchSense;
      this.sightTargetType = sightTargetType;
      this.sightAngle = sightAngle;
      this.sightSense = sightSense;
      this.sightHasTarget = sightHasTarget;
      
      smellDistanceModifier = 0.5;
      baseSensorRange = 10;
   }
   
   //-----------------------------------------------------------------settings
   public void nextSightTarget() {
	   this.nextTargetScheduled = true;
	   this.prevTargetScheduled = false;
   }
   public void prevSightTarget() {
	   this.nextTargetScheduled = false;
	   this.prevTargetScheduled = true;
   }
   
   //-----------------------------------------------------------------utilities
   public static int findGreatest(int[] ara){
      return findGreatest(ara,1.0);
   }
   
   public static int findGreatest(int[] ara, double debouncer){
      //returns -1 all are 0
      int max = 0;
      int dir = -1;
      for(int i = 0; i < ara.length; i++)
         if(ara[i] > max) {
            max = (int)(ara[i] * debouncer);
            dir = i;
         }
      
      return dir;
   }
   
   public static int findFirst(boolean[] ara){
      for(int i = 0; i < ara.length; i++)
         if(ara[i])
            return i;
      
      return -1;
   }
   
   public static int getAverage(int[] ara){
      int total = 0;
      for(int i = 0; i < ara.length; i++)
         total += ara[i];
      
      return total / ara.length;
   }
   
   public static int getNumber(boolean[] ara){
      int total = 0;
      for (int i = 0; i < ara.length; i++)
         if(ara[i])
            total += 1;
      
      return total;
   }
   
   //-----------------------------------------------------------------getters
   public int[] getHearingSense(){return this.hearingSense;}
   public int[] getEnergySmellSense(){return this.energySmellSense;}
   public int[] getAllySmellSense(){return this.allySmellSense;}
   public int[] getEnemySmellSense(){return this.enemySmellSense;}
   public int[] getObstructionTouchSense(){return this.obstructionTouchSense;}
   public int[] getEnergyTouchSense(){return this.energyTouchSense;}
   public boolean[] getAllyTouchSense(){return this.allyTouchSense;}
   public boolean[] getEnemyTouchSense(){return this.enemyTouchSense;}
   public boolean getSightHasTarget(){return this.sightHasTarget;}
   public int getSightAngle(){return this.sightAngle;}
   public int getSightSense(){return this.sightSense;}
   public SightTargetType getSightTargetType(){return this.sightTargetType;}
   public int getSightXpos() {	//will return an X coordinate if you arent trying to see a non-allied robit
	   if(this.sightTarget.getTargetType() != SightTargetType.ENEMY && this.sightTarget.getTargetType() != SightTargetType.ROBIT)
		   return this.sightTarget.getXpos();
	   return -1;
   }
   public int getSightYpos() {	//will return an Y coordinate if you arent trying to see a non-allied robit
	   if(this.sightTarget.getTargetType() != SightTargetType.ENEMY && this.sightTarget.getTargetType() != SightTargetType.ROBIT)
		   return this.sightTarget.getYpos();
	   return -1;
   }
   
   //-----------------------------------------------------------------setters
   public void setHearingSense(int[] h){this.hearingSense = h;}
   public void setEnergySmellSense(int[] fs){this.energySmellSense = fs;}
   public void setAllySmellSense(int[] as){this.allySmellSense = as;}
   public void setEnemySmellSense(int[] es){this.enemySmellSense = es;}
   public void setObstructionTouchSense(int[] os){this.obstructionTouchSense = os;}
   public void setEnergyTouchSense(int[] fs){this.energyTouchSense = fs;}
   public void setAllyTouchSense(boolean[] as){this.allyTouchSense = as;}
   public void setEnemyTouchSense(boolean[] es){this.enemyTouchSense = es;}
   public void setSightHasTarget(boolean t){this.sightHasTarget = t;}
   public void setSightAngle(int a){this.sightAngle = a;}
   public void setSightSense(int s){this.sightSense = s;}
   public void setSightTargetType(SightTargetType t){ this.sightTargetType = t;}

   
   //====================================================================================================
   //==================================Not for use by Players============================================
   //====================================================================================================
   
   
   public void updateSenses(World sim, Robit c) {  //sorry to anyone trying to read this. needed it to run fast AF so i klueged the whole thing into the one loop set
	   int senses,senseBuff,senseDistance,x,y,dx,dy,xVal,yVal,otherStealth,otherStealthAddition;
		Robit[][] robitMap = sim.getRobitMap();
		int[][] energyMap = sim.getEnergyMap();
		Obstruction[][] obstructionMap = sim.getObstructionMap();
		
		this.hearingSense = new int[4];
		this.energySmellSense = new int[4];
		this.enemySmellSense = new int[4];
		this.allySmellSense = new int[4];
		this.allyTouchSense = new boolean[4];
		this.enemyTouchSense = new boolean[4];
		this.obstructionTouchSense = new int[4];
		this.energyTouchSense = new int[5];
		
		senses = c.getSense();                          	//sense stat
		x = sim.fc(c.getXpos());                        	//xpos of Robit
		y = sim.fc(c.getYpos());                            //ypos of Robit
		senseBuff = c.getSenseBuff();
		senseDistance = (int)((baseSensorRange + (senses-100)/10.0)*((100+senseBuff)/100.0)) + 1;//max sensory distance
		
		LinkedList<SightTarget> sightList = new LinkedList<>();
		
		int smellMaxRange;
		
		//this stops the scanning from scannning the same square twice in small map sizes
		int startX,startY, endX, endY;
		if(senseDistance > sim.WORLD_SIZE/2) {
			startX = x - (sim.WORLD_SIZE/2);
			startY = y - (sim.WORLD_SIZE/2);
			endX = x + (sim.WORLD_SIZE/2);
			endY = y + (sim.WORLD_SIZE/2);
		}else {
			startX = x - senseDistance;
			startY = y - senseDistance;
			endX = x + senseDistance;
			endY = y + senseDistance;
		}
			
	    for(int xv = startX;xv <= endX;xv++){ //loop through a box around the Robit checking each square's contents
			for(int yv = startY;yv <= endY;yv++){
				xVal = sim.fc(xv);                        		//the currently scanning square cords after wrapping sides
				yVal = sim.fc(yv); 
				dx = x-xv;               		                //the difference in xvals
				dy = y-yv;                                		//the difference in yvals

				smellMaxRange = (int)(senseDistance * smellDistanceModifier); 	//smell is short range, factor in the range limiter
					
				//if we found a Robit other than the current Robit
				if(robitMap[yVal][xVal] != null && (xVal != x && yVal != y)){   //handle if tile contains Robit
					Robit o = robitMap[yVal][xVal];                             //the Robit in scanning range
					otherStealth = o.getStealth();                              //others stealth stat
					otherStealthAddition = (otherStealth-100)/10;
					if(c.isAlly(o.getSpecies())){
						populateSmellSenseArray(allySmellSense,smellMaxRange,dx,dy,1);
						if(this.sightTargetType == SightTargetType.ALLY) {
							SightTarget t = new SightTarget(SightTargetType.ALLY,c,xVal,yVal,new GameObject(o));
							sightList.add(t);
						}
					}
					else{                       
						populateSmellSenseArray(enemySmellSense,smellMaxRange,dx+otherStealthAddition,dy+otherStealthAddition,1);
						
						if(this.sightTargetType == SightTargetType.ENEMY) {
							SightTarget t = new SightTarget(SightTargetType.ENEMY,c,xVal,yVal,new GameObject(o));
							sightList.add(t);
						}
					} 
					populateSmellSenseArray(hearingSense,senseDistance,dx+otherStealthAddition,dy+otherStealthAddition,1);
					if(this.sightTargetType == SightTargetType.ROBIT) {
						SightTarget t = new SightTarget(SightTargetType.ROBIT,c,xVal,yVal,new GameObject(o));
						sightList.add(t);
					}
				}
				//if there is a energy patch in the selected square
				if(energyMap[yVal][xVal] > 0){
					populateSmellSenseArray(energySmellSense,smellMaxRange,dx,dy,energyMap[yVal][xVal]/50.0);
					if(this.sightTargetType == SightTargetType.ENERGY) {
						SightTarget t = new SightTarget(SightTargetType.ENERGY,c,xVal,yVal,new GameObject(energyMap[yVal][xVal]));
						if(!sightList.contains(t) && !t.update(senseDistance, sim))
							sightList.add(t);
					}
				}
				
				if(!obstructionMap[y][x].isEmpty()) {
					if(this.sightTargetType == SightTargetType.OBSTRUCTION) {
						SightTarget t = new SightTarget(SightTargetType.OBSTRUCTION,c,xVal,yVal,new GameObject(obstructionMap[yVal][xVal]));
						sightList.add(t);
					}
				}
			}
		}  
		//populate touch senses
		AOE myAOE = AOE.ADJACENT;
		for(int i = 0; i < myAOE.locations.length;i++){ 
			Robit selectC = robitMap[sim.fc(y+myAOE.locations[i].yMod)][sim.fc(x+myAOE.locations[i].xMod)];
			Obstruction selectO = obstructionMap[sim.fc(y+myAOE.locations[i].yMod)][sim.fc(x+myAOE.locations[i].xMod)];         
			if(selectC != null){
				if(selectC.getSpecies().equals(c.getSpecies()))
					allyTouchSense[i] = true;
				else
					enemyTouchSense[i] = true;
			}
			energyTouchSense[i] = energyMap[sim.fc(y+myAOE.locations[i].yMod)][sim.fc(x+myAOE.locations[i].xMod)];
			if(selectO.isEmpty())
				obstructionTouchSense[i] = 0;
			else
				obstructionTouchSense[i] = selectO.getHP();
		}   
		
		if(sightList.isEmpty()) 
			clearSight();
		else {
			if(sightTarget == null && !this.prevTargetScheduled && !this.nextTargetScheduled)
				this.nextTargetScheduled = true;
			//if they are trying to change the sight target
			if(this.nextTargetScheduled || this.prevTargetScheduled) {
				Collections.sort(sightList);
				if(sightTarget == null && nextTargetScheduled)
					populateSight(sightList.getFirst(),c,senseDistance);
				else if(sightTarget == null && prevTargetScheduled)
					populateSight(sightList.getLast(),c,senseDistance);
				else if(sightTarget != null && !sightList.contains(sightTarget))
					clearSight();
				else if(sightTarget != null && sightList.contains(sightTarget)) 
					refocusSight(sightList.indexOf(sightTarget),sightList, c, senseDistance);
			}
			nextTargetScheduled = false;
			prevTargetScheduled = false;
			
			if(this.sightTarget != null && this.sightTarget.update(senseDistance, sim)) {
				clearSight();
			}
			else {
				dx = c.getXpos() - sightTarget.getXpos();
				dy = c.getYpos() - sightTarget.getYpos();
				//System.out.println("(" + sightTarget.getXpos() + "," + sightTarget.getYpos() + ") (" + dx + "," + dy + ") " + sightTarget.getAngle());
			}
		}
		
   }
   
   private void refocusSight(int startIndex, LinkedList<SightTarget> targets, Robit c, int senseDistance) {
	   if(startIndex == 0 && this.prevTargetScheduled) 
		   populateSight(targets.getLast(),c,senseDistance);
	   else if(startIndex == targets.size()-1 && nextTargetScheduled) 
		   populateSight(targets.getFirst(),c,senseDistance);
	   else {
		   if(nextTargetScheduled)
			   populateSight(targets.get(startIndex+1),c,senseDistance);
		   else if(prevTargetScheduled)
			   populateSight(targets.get(startIndex-1),c,senseDistance);
	   }
   }
   
   private void clearSight() {
	   this.sightHasTarget = false;
		this.sightAngle = 0;
		this.sightSense = 0;
		this.sightTarget = null;
   }
   
   private void populateSight(SightTarget t, Robit s, int sensorRange) {
	   this.sightTarget = t;
	   this.sightAngle = t.getAngle();
	   this.sightSense = t.getActivity();
	   this.sightHasTarget = true;
   }
   
 //-----------------------------------------------------------------checkLOS
   public static boolean checkLOS(int x0, int y0, int x1, int y1, World sim) {
	   double step = 0.2;
	   if(x0 > x1)
		   step = -0.2;
	   
	   for(double x = x0; Math.floor(x) != x1; x += step) {
		   if(step < 0 && Math.floor(x) <= x1)
			   return true;
		   if(step > 0 && Math.floor(x) >= x1)
			   return true;
		   
		   double y = ((x - x0) / (x1 - x0)) * (y1-y0) + y0;
		   
		   int xVal = sim.fc((int)x);
		   int yVal = sim.fc((int)y);
		   
		   if(sim.getObstructionMap()[yVal][xVal].getType().BLOCKS_AOE) {
			   return true;
		   }
	   }
	   
	   return false;
   }
   

 	//-----------------------------------------------------------------populateSmellSenseArray
 	private void populateSmellSenseArray(int[] ara, int maxRange, int dx, int dy, double muiltiplier){
 		double d = Math.sqrt(Math.pow(dx,2)+Math.pow(dy,2));

 		if(Math.abs(dy) <= Math.abs(dx) && dx > 0)
 			ara[3] +=(int)(calcActivity(maxRange,d-1)*muiltiplier);
 		if(Math.abs(dy) <= Math.abs(dx) && dx < 0)
 			ara[1] +=(int)(calcActivity(maxRange,d-1)*muiltiplier);     
 		if(Math.abs(dy) >= Math.abs(dx) && dy > 0)
 			ara[0] +=(int)(calcActivity(maxRange,d-1)*muiltiplier);
 		if(Math.abs(dy) >= Math.abs(dx) && dy < 0)
 			ara[2] +=(int)(calcActivity(maxRange,d-1)*muiltiplier);
 		
 		for(int i = 0; i < ara.length; i++) {
 			if(ara[i] > 100) ara[i] = 100;
 			if(ara[i] < 0) ara[i] = 0;
 		}

 	}

 	//-----------------------------------------------------------------calcActivity
 	public static int calcActivity(int maxSensorRange, double measuredRange){
 		int activity = (int)(-1.0 * measuredRange * (100.0/maxSensorRange) + 100.0);
 	
 		//activity = (int)(-1* Math.pow(measuredRange - maxSensorRange,3) * (100.0/Math.pow(maxSensorRange,3)));
 		
 		if(activity < 0)
 			activity = 0;
 		if(activity > 100)
 			activity = 100;
 		return activity;
 	}













}