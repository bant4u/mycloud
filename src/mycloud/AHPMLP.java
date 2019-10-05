/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mycloud;

/**
 *
 * @author bijay
 */
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.events.LearningEvent;
import org.neuroph.core.events.LearningEventListener;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.MomentumBackpropagation;
import org.neuroph.util.TransferFunctionType;


public class AHPMLP {
private static List<Cloudlet> cloudletList;
private static List<Cloudlet> sortedCloudletList;
private static List<Vm> vmlist;
private static DecimalFormat df = new DecimalFormat(".###");
private static int vmNo = 3;
private static int cloudletNo = 4;
//creating resource comparison matrix
//input ‐2 for missing data here.
private static double[] vmMatrixData = new double[]{3,5,-2};
//creating task comparison matrices    
//input ‐2 for missing data here.
// private static double[][] cloudletMatrixData = new double[][]{
//    {1/2.0, 4, 1/6.0, 4, 1/4.0, 1/5.0},
//    {1/2.0, 1/3.0, 5, 1/2.0, 7, 9},
//    {1/5.0, 1, 1/2.0, 1/2.0, 2, 3}
// };
private static double[][] cloudletMatrixData = new double[][]{
    {1/2.0, -2, 1/6.0, 4,-2, 1/5.0},
    {1/2.0, 1/3.0, 5, -2, 7, 9},
    {1/5.0, 1, -2, 1/2.0, 2, 3}
};
private static int missingElementNoForResourceMatrix = 1;
private static int[] missingElementNoForTaskMatrix = new int[]{2,1,1};
private static int defaultnoOfTrainingSample = 30;
// private static int matrixOrder = 6;
// private static int missingNumberOfElements = 4;
// private static int noOfTrainingInputs = 30;
//
private static List<Vm> createVM(int userId, int vms) {
    // Creates a container to store VMs. This list is passed to the broker
    // later
    LinkedList<Vm> list = new LinkedList<Vm>();
    // VM Parameters
    long size = 10000; // image size (MB)
    int ram = 512; // vm memory (MB)
    int mips = 1000;
    long bw = 1000;
    int pesNumber = 1; // number of cpus
    String vmm = "Xen"; // VMM name
    // create VMs
    Vm[] vm = new Vm[vms];
    for (int i = 0; i < vms; i++) {
    vm[i] = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new
    CloudletSchedulerTimeShared());
    // for creating a VM with a space shared scheduling policy for
    // cloudlets:
    // vm[i] = Vm(i, userId, mips, pesNumber, ram, bw, size, priority,
    // vmm, new CloudletSchedulerSpaceShared());
    list.add(vm[i]);
    }
    return list;
}
private static List<Cloudlet> createCloudlet(int userId, int cloudlets) {
    // Creates a container to store Cloudlets
    LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
    // cloudlet parameters
    long length = 1000;
    long fileSize = 300;
    long outputSize = 300;
    int pesNumber = 1;
    UtilizationModel utilizationModel = new UtilizationModelFull();
    Cloudlet[] cloudlet = new Cloudlet[cloudlets];
    for (int i = 0; i < cloudlets; i++) {
    cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel,
utilizationModel,
      utilizationModel);
    // setting the owner of these Cloudlets
    cloudlet[i].setUserId(userId);
    list.add(cloudlet[i]);
    }
    return list;
}
private static Datacenter createDatacenter(String name){

    // Here are the steps needed to create a PowerDatacenter:
    // 1. We need to create a list to store one or more
    //    Machines
    List<Host> hostList = new ArrayList<Host>();
    // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
    //    create a list to store these PEs before creating
    //    a Machine.
    List<Pe> peList1 = new ArrayList<Pe>();
    int mips = 1000;
    // 3. Create PEs and add these into the list.
    //for a quad‐core machine, a list of 4 PEs is required:
    peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
    peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
    peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
    peList1.add(new Pe(3, new PeProvisionerSimple(mips)));
    //Another list, for a dual‐core machine
    List<Pe> peList2 = new ArrayList<Pe>();
    peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
    peList2.add(new Pe(1, new PeProvisionerSimple(mips)));
    //4. Create Hosts with its id and list of PEs and add them to the list of machines
    int hostId=0;
    int ram = 2048; //host memory (MB)
    long storage = 1000000; //host storage
    int bw = 10000;
    hostList.add(
     new Host(
       hostId,
       new RamProvisionerSimple(ram),
       new BwProvisionerSimple(bw),
       storage,
       peList1,
       new VmSchedulerTimeShared(peList1)
       )
     ); // This is our first machine
    hostId++;
    hostList.add(
     new Host(
       hostId,
       new RamProvisionerSimple(ram),
       new BwProvisionerSimple(bw),
       storage,
       peList2,
       new VmSchedulerTimeShared(peList2)
       )
     ); // Second machine
    //To create a host with a space‐shared allocation policy for PEs to VMs:
    //hostList.add(
    //    new Host(
    //     hostId,
    //     new CpuProvisionerSimple(peList1),
    //     new RamProvisionerSimple(ram),
    //     new BwProvisionerSimple(bw),
    //     storage,
    //     new VmSchedulerSpaceShared(peList1)
    //    )
    // );
    //To create a host with a opportunistic space‐shared allocation policy for PEs to VMs:
    //hostList.add(
    //    new Host(
    //     hostId,
    //     new CpuProvisionerSimple(peList1),
    //     new RamProvisionerSimple(ram),

    //     new BwProvisionerSimple(bw),
    //     storage,
    //     new VmSchedulerOportunisticSpaceShared(peList1)
    //    )
    // );
    // 5. Create a DatacenterCharacteristics object that stores the
    //    properties of a data center: architecture, OS, list of
    //    Machines, allocation policy: time‐ or space‐shared, time zone
    //    and its price (G$/Pe time unit).
    String arch = "x64";      // system architecture
    String os = "Linux";          // operating system
    String vmm = "Xen";
    double time_zone = 10.0;         // time zone this resource located
    double cost = 3.0;              // the cost of using processing in this resource
    double costPerMem = 0.05;    // the cost of using memory in this resource
    double costPerStorage = 0.1; // the cost of using storage in this resource
    double costPerBw = 0.1;     // the cost of using bw in this resource
    LinkedList<Storage> storageList = new LinkedList<Storage>(); //we are not adding SAN devices

    DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
     arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
    // 6. Finally, we need to create a PowerDatacenter object.
    Datacenter datacenter = null;
    try {
    datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList),
storageList, 0);
    } catch (Exception e) {
    e.printStackTrace();
    }
    return datacenter;
}
private static DatacenterBroker createBroker(){
    DatacenterBroker broker = null;
    try {
    broker = new DatacenterBroker("Broker");
    } catch (Exception e) {
    e.printStackTrace();
    return null;
    }
    return broker;
}
private static void printCloudletList(List<Cloudlet> list) {
    int size = list.size();
    Cloudlet cloudlet;
    String indent = "    ";
    Log.printLine();
    Log.printLine("========== OUTPUT ==========");
    Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
     "Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time"+ indent + "Finish Time");
    DecimalFormat dft = new DecimalFormat("###.##");
    for (int i = 0; i < size; i++) {
    cloudlet = list.get(i);
    Log.print(indent + cloudlet.getCloudletId() + indent + indent);
    if (cloudlet.getCloudletStatus()== Cloudlet.SUCCESS){
     Log.print("SUCCESS");
     Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent +
cloudlet.getVmId() +
       indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
       indent + indent + dft.format(cloudlet.getExecStartTime())+ indent + indent +
indent + dft.format(cloudlet.getFinishTime()));
    }
    }
}
private static RealMatrix NNProcess(RealMatrix inputMatrix, int matrixOrder, int
missingNumberOfElements, int noOfTrainingSample ){
    int inputsize = matrixOrder * (matrixOrder - 1) / 2;
    int outputsize = inputsize;
    DataSet dataset = new DataSet(inputsize, outputsize);
    try {
    if(missingNumberOfElements > inputsize){
     throw new Exception("missing elements exceeded the input size.");
    }
    for (int i = 0; i < noOfTrainingSample; i++) {
     //step1 : preparing MLP input
     RealMatrix m = MatrixManipulations.createRandomInconsistentMatrix(matrixOrder,
missingNumberOfElements);
     double[] input = MatrixManipulations.extractUpperTriangularMatrixData(m);
     //MatrixManipulations.printMatrix(m);
     //System.out.println(Arrays.toString(input));
     double[] normalizedInput = MatrixManipulations.normalizeVector(input);
     //System.out.println(Arrays.toString(normalizedInput));
     //double[] denormalizedInput = MatrixManipulations.deNormalizeVector(normalizedInput);
     //System.out.println(Arrays.toString(denormalizedInput));
     //System.out.println("=====");
     //step 2 : preparing MLP output target vectors
     RealMatrix mTarget =  MatrixManipulations.createRandomInconsistentMatrix(matrixOrder,0);        
     double cr = MatrixManipulations.computeMatrixCR(mTarget);
     double min = cr;
     while (cr > 0.10) {
      mTarget = MatrixManipulations.createRandomInconsistentMatrix(matrixOrder,0);
      cr = MatrixManipulations.computeMatrixCR(mTarget);          
      if(cr<min) min = cr;
      //System.out.println("CR:"+cr + "\t MIN:"+min);
     }
     double[] output = MatrixManipulations.extractUpperTriangularMatrixData(mTarget);
     //System.out.println(Arrays.toString(output));
     double[] normalizedOutput = MatrixManipulations.normalizeVector(output);
     //System.out.println(Arrays.toString(normalizedOutput));
     //dataset.addRow(normalizedInput, normalizedOutput);
     //dataset.addRow(normalizedInput,normalizedOutput);
     dataset.add(normalizedInput, normalizedOutput);
     
    } // endfor
    //neural network setup with momentum backpropagation
    MomentumBackpropagation mbpn = new MomentumBackpropagation();
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(TransferFunctionType.TANH, inputsize,
30, outputsize);
    mlp.reset();    
    mbpn.setMomentum(0.01);                
    mbpn.setLearningRate(0.01);
    mbpn.setMaxIterations(10000);
    mbpn.setMaxError(0.001);
    mlp.setLearningRule(mbpn);
    mbpn.addListener(new LearningEventListener() {
     @Override
     public void handleLearningEvent(LearningEvent event) {
      MomentumBackpropagation mbp =(MomentumBackpropagation)event.getSource();
      //         System.out.println(mbp.getCurrentIteration() + ". iteration |Total network error: "
      //             + mbp.getTotalNetworkError());
     }
    });
    mlp.learn(dataset);

    // testing new input data now..
    //RealMatrix m1 =
MatrixManipulations.createRandomInconsistentMatrix(matrixOrder,missingNumberOfElements);
    //MatrixManipulations.printMatrix(inputMatrix);
    //System.out.println("Before CR:" + MatrixManipulations.computeMatrixCR(inputMatrix));
    double[] input1 = MatrixManipulations.extractUpperTriangularMatrixData(inputMatrix);
    //System.out.println(Arrays.toString(input1));
    double[] normalizedInput1 = MatrixManipulations.normalizeVector(input1);
    //System.out.println(Arrays.toString(normalizedInput1));
    mlp.setInput(normalizedInput1);
    mlp.calculate();
    //System.out.println("====================OUTPUT===============");
    double[] output1 = mlp.getOutput();
    //System.out.println(Arrays.toString(output1));
    double[] denormalizedOutput1 = MatrixManipulations.deNormalizeVector(output1);
    //System.out.println(Arrays.toString(denormalizedOutput1));
    RealMatrix m2 =
MatrixManipulations.completeMatrixWithLowerTraingularData(denormalizedOutput1, matrixOrder);
    //MatrixManipulations.printMatrix(m2);
    //System.out.println("After CR:" + MatrixManipulations.computeMatrixCR(m2));
    return m2;
    } catch (Exception e) {
    System.out.println("Exception in main.\n");
    return null;
    }
}
public static void main(String[] args) {
    Log.printLine("Starting CloudSim simulation...");
    // creating 4 cloudlets and 3 vms
    try {
    // First step: Initialize the CloudSim package. It should be called
    // before creating any entities.
    int num_user = 1; // number of cloud users
    Calendar calendar = Calendar.getInstance();
    boolean trace_flag = false; // mean trace events
    // Initialize the CloudSim library
    CloudSim.init(num_user, calendar, trace_flag);
    // Second step: Create Datacenters
    // Datacenters are the resource providers in CloudSim. We need at
    // list one of them to run a CloudSim simulation
    @SuppressWarnings("unused")
    Datacenter datacenter0 = createDatacenter("Datacenter_0");
    //     @SuppressWarnings("unused")
    //     Datacenter datacenter1 = createDatacenter("Datacenter_1");    
    //Third step: Create Broker
    DatacenterBroker broker = createBroker();
    int brokerId = broker.getId();
    //Fourth step: Create VMs and Cloudlets and send them to broker    
    vmlist = createVM(brokerId,vmNo); //creating 3 vms
    //vmlist = createVM(brokerId,1);
    cloudletList = createCloudlet(brokerId,cloudletNo); // creating 4 cloudlets
    System.out.println(vmNo+" resources (VMs) and "+cloudletNo + " tasks (cloudlets)successfully created as desired.");
    //creating resource comparison matrix   
    RealMatrix vmMatrix =
MatrixManipulations.completeMatrixWithLowerTraingularData(vmMatrixData, vmNo);
    System.out.println("Resource comparison matrix created:");
    MatrixManipulations.printMatrix(vmMatrix);   
    if(missingElementNoForResourceMatrix ==0)
     System.out.println("CR:"+ MatrixManipulations.computeMatrixCR(vmMatrix));
    //System.out.println("Lambda‐Max :"+MatrixManipulations.lambdaMax);
    while(MatrixManipulations.computeMatrixCR(vmMatrix) > 0.1 ||
(missingElementNoForResourceMatrix > 0)){
     System.out.println("Training with MLP to correct CR of the resource comparison matrix...");
     vmMatrix = NNProcess(vmMatrix, vmNo, missingElementNoForResourceMatrix,defaultnoOfTrainingSample);        
     MatrixManipulations.printMatrix(vmMatrix);        
     System.out.println("CR:"+ MatrixManipulations.computeMatrixCR(vmMatrix));
     missingElementNoForResourceMatrix = 0;
    }
    //creating task comparison matrices    
    RealMatrix[] cloudletMatrix = new RealMatrix[vmNo];      
    System.out.println("\nFollowing task comparison matrices are created :");
    for(int i=0;i<vmNo;i++){                
     cloudletMatrix[i] =
MatrixManipulations.completeMatrixWithLowerTraingularData(cloudletMatrixData[i],cloudletNo);
     System.out.println("\n=============TASK COMPARISON MATRIX("+(i+1)+")=================");
     MatrixManipulations.printMatrix(cloudletMatrix[i]);
     if(missingElementNoForTaskMatrix[i] == 0)
      System.out.println("CR:"+ MatrixManipulations.computeMatrixCR(cloudletMatrix[i]));
     while(MatrixManipulations.computeMatrixCR(cloudletMatrix[i]) > 0.1 ||
(missingElementNoForTaskMatrix[i] >0)){          
      System.out.println("Training with MLP to correct CR of the task comparison matrix...");
      cloudletMatrix[i] = NNProcess(cloudletMatrix[i], cloudletNo,
missingElementNoForTaskMatrix[i], defaultnoOfTrainingSample);
      System.out.println("Corrected Matrix after training");
      MatrixManipulations.printMatrix(cloudletMatrix[i]);
      System.out.println("New CR:"+
MatrixManipulations.computeMatrixCR(cloudletMatrix[i]));   
      missingElementNoForTaskMatrix[i] =0;
     }
    }
    //assuming all matrices are consistent now
    RealVector[] cloudletVector = new RealVector[vmNo];
    for(int i=0;i<vmNo;i++){        
     cloudletVector[i] = MatrixManipulations.computeEigenVector(cloudletMatrix[i]);   
   
    }
    int rows = cloudletNo;
    int cols = vmNo;
    double[][] deltaMatrixData = new double[rows][cols];
    RealMatrix deltaMatrix = MatrixUtils.createRealMatrix(deltaMatrixData);        
   
    for(int i=0;i<cloudletVector.length;i++){
     deltaMatrix.setColumnVector(i, cloudletVector[i]);
    }
    System.out.println("\nPrinting delta Matrix:");
    MatrixManipulations.printMatrix(deltaMatrix);
    RealVector gamma = MatrixManipulations.computeEigenVector(vmMatrix);

    RealMatrix pvs =
deltaMatrix.multiply(MatrixUtils.createColumnRealMatrix(gamma.toArray()));
    System.out.println("\nPrinting PVS Matrix:");
    MatrixManipulations.printMatrix(pvs);
    System.out.println("The selected job was : job"+(pvs.getColumnVector(0).getMaxIndex()+"\n"));
    double[] pvsData = pvs.getColumnVector(0).toArray();
    double[] sortedPvsData = pvsData.clone();    
    Arrays.sort(sortedPvsData);
    sortedCloudletList = new LinkedList<Cloudlet>();
    for(int i=sortedPvsData.length-1;i>=0;i--){
     for(int j=0;j<pvsData.length;j++){
      if(sortedPvsData[i] == pvsData[j]){
       sortedCloudletList.add(cloudletList.get(j));
      }
     }        
    }
    broker.submitVmList(vmlist);
    broker.submitCloudletList(sortedCloudletList);
    // Fifth step: Starts the simulation
    CloudSim.startSimulation();
    // Final step: Print results when simulation is over
    List<Cloudlet> newList = broker.getCloudletReceivedList();
    CloudSim.stopSimulation();
    printCloudletList(newList);
    Log.printLine("Simulation finished!");
    } catch (Exception e) {
    e.printStackTrace();
    Log.printLine("The simulation has been terminated due to an unexpected error");
    }
}
}