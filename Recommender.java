import javafx.util.Pair;

import java.io.*;
import java.sql.*;
import java.util.*;


class DoubleValue{
    double d = 0;
    public void setValue(double d){
        this.d = d;
    }
    public void incrementValue(double d){
        this.d += d;
    }
    public double getValue(){
        return d;
    }

    public DoubleValue(double d){
        this.d = d;
    }
}

class ItemRecord{
    public int itemID;
    public int average;
    //public long date;
    public int userID;

//    @Override
//    public String toString() {
//        return "("+ itemID + "," + average + "," + date + "," + userID + ")";
//    }

    @Override
    public int hashCode() {
        return new Integer(itemID).hashCode();
    }

//    @Override
//    public boolean equals(Object obj) {
//        if ( !(obj instanceof  ItemRecord)){
//            return false;
//        }
//        return itemID==(((ItemRecord) obj).itemID) && average==((ItemRecord) obj).average;
//    }


    public ItemRecord(int itemID, int average, int userID){
        this.itemID = itemID;
        this.average = average;
        //this.date = date;
        this.userID = userID;
    }

}

class SimRecord{
    public int user;
    public int i1;
    public int i2;
    public double avg, rating1, rating2;
    public int date1, date2;

    public SimRecord(int user, int i1, int i2, double avg, double rating1, double rating2, int date1, int date2){
        this.user = user;
        this.i1 = i1;
        this.i2 = i2;
        this.avg = avg;
        this.rating1 = rating1;
        this.rating2 = rating2;
        this.date1 = date1;
        this.date2 = date2;
    }
}




public class Recommender {

    public static final String DATABASE_URL = "jdbc:mysql://localhost:3306/socialdb";
    public static final String DATABASE_USERNAME = "root";
    public static final String DATABASE_PASSWORD = "*******";


    public HashMap<Integer, Pair<List<ItemRecord>,DoubleValue>> userToRecords;
    public HashMap<Integer, Pair<HashMap<Integer, Integer>, Pair<DoubleValue, DoubleValue>>> itemToRecords;


    public List<SimRecord> records;
    public HashMap<Integer, HashMap<Integer, DoubleValue>> similarityMatrix;


    public double userRating(int user, int item){
        Pair<List<ItemRecord>, DoubleValue> itemsValue = userToRecords.get(user);
        double average = findAverage2(user);
        if (itemsValue == null ){
            return average;
        }
        for (ItemRecord i: itemsValue.getKey()) {
            if (i.itemID == item) {
                return (double) i.average;
            }
        }
        return average;
    }

    public Pair<Double, Double> getTimestamp(int item){
        Pair<HashMap<Integer, Integer>, Pair<DoubleValue, DoubleValue>> a = itemToRecords.get(item);
        if(a == null){
            return null;
        }
        return new Pair<>(a.getValue().getKey().getValue(), a.getValue().getValue().getValue());
    }

    public static double similarity(double a, double b){
        return Math.min(a,b) / Math.max(a, b);
    }

    public double recent(int item1, int item2, int range_type){
        Pair<Double, Double> one = getTimestamp(item1);
        Pair<Double, Double> two = getTimestamp(item2);
        if(one == null || two == null){
            return 0;
        }
        double score = (similarity(one.getKey(), two.getKey()) + similarity(one.getValue(), two.getValue()))/2;
        if(range_type == 0){
            // 0 to 1 <--> -1 to 1
            // x * 2 - 1
            return (score * 2) - 1;
        }
        return score;
    }

    public void runExperiment(List<Integer> kValues, int total, double[] sim, int[] indexes){


        int t = kValues.size() * 3 * 5 * 2;
        int count = 0;

        for(Integer k: kValues){

            for(int metric=0; metric<3; metric++){

                for(int cold_start=0; cold_start<5; cold_start++){

                    for(int range=0; range<2; range ++){
                        count += 1;
                        System.out.println("\n\n Experiment runs: " + count + " / " + t + "\n\n");

                        Pair<Double, Double> data = experiment_run(sim, indexes, k, total, metric, cold_start, range);

                        addToExperimentCSV(k, metric, cold_start, range, data.getKey(), data.getValue());

                    }

                }

            }

        }



    }

    public static void saveArrays(double[] sim, int[] index) throws Exception{
        String file1 = "/Users/faithnyota1/trainingdata/sim.ser";
        String file2 = "/Users/faithnyota1/trainingdata/index.ser";

        ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(file1)
        );
        out.writeObject(sim);
        out.flush();
        out.close();

        out = new ObjectOutputStream(
                new FileOutputStream(file2)
        );
        out.writeObject(index);
        out.flush();
        out.close();
    }

    public PriorityQueue<Pair<Integer, Double>> getNeighbours(int item, double[] sim, int[] index, int k, int type, int range_type){
        PriorityQueue<Pair<Integer,Double>> queue = new PriorityQueue<>(new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        double s;
        double sm = 0;
//        double low = 10;
//        double average = 0;
//        int count = 0;
        for(int i=1; i<=19999; i++){
            if(i != item){

                /*
                Type
                0 = highest
                1 = recent
                2 = highest and recent
                 */

                /*
                Range type
                0 = [-1 to 1]
                1 = [0 to 1]

                 */

                if(type == 0 || type == 2){
                    if(i < item){
                        sm = sim[getIndex(i, item, index)];
                    }else{
                        sm = sim[getIndex(item, i, index)];
                    }
                    sm = Double.isNaN(sm) ? 0 : sm;

                    if(range_type==1){
                        sm = (sm + 1)/2;
                    }


                }


                if( type == 0){
                    s = sm;
                }else{
                    s = this.recent(i, item, range_type);
                    if(type == 2){
                        s = (s + sm)/2;
                    }
                }









//               average += s;
//               count += 1;
               Pair<Integer,Double> pair = new Pair<>(i, s);
               queue.add(pair);
               if(queue.size() > k){
                   queue.poll();
               }
            }
        }
        return queue;
    }


    public double itemAverage(int item, int average_type){
        /*
        Average Type
        0 - 2
        1 - 2.5
        2 - 3
         */
        double tot = 0;
        int count = 0;
        HashMap<Integer, Integer> result = itemToRecords.get(item).getKey();
        if(result == null){
            return (2 + average_type*0.5);
        }
        for (Integer user : result.keySet()) {
            count += 1;
            tot += itemToRecords.get(item).getKey().get(user);
        }
        return tot/count;
    }


    public int prediction2(int user, int item, PriorityQueue<Pair<Integer,Double>> neighbours, int cold_start_type){
        double predNum = 0;
        double predDenom = 0;
        double temp;
        for(Pair<Integer, Double> pair: neighbours){
            temp = pair.getValue();
            predNum += temp * userRating(user, pair.getKey());
            predDenom += temp;
        }
        /*
        Cold start type
        0 - 2
        1 - 2.5
        2 - 3
        3 - always 2
        4 - always 3
         */

        if (predDenom == 0){
            if(cold_start_type > 2){
                return (cold_start_type - 1);
            }
            int r = (int) Math.round(itemAverage(item,cold_start_type));
            return r;
        }

        double pred = predNum / predDenom;
        return Math.max((int) Math.round(pred), 1);
    }


    public static void addToCSV(int user, int item, int rating, int timestamp){
        String csv = "/Users/faithnyota1/trainingdata/prediction.csv";
        try {
            FileWriter pw = new FileWriter(csv,true);
            pw.append(user + "," + item + "," + rating + "," + timestamp + "\n" );
            pw.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void addToExperimentCSV(int k, int metric, int cold_start, int range, double mse, double matchPercentage){
        String csv = "/Users/faithnyota1/trainingdata/experiment.csv";
        try {
            FileWriter pw = new FileWriter(csv,true);
            pw.append(k + "," + metric + "," + cold_start + "," + range + "," + mse + "," + matchPercentage + "\n" );
            pw.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }


    public Pair<Double, Double> experiment_run(double[] sim, int[] indexes, int k, int total, int metric_type, int cold_type, int range_type){

        int count = 0;
        double rmse = 0;
        int number_exact = 0;
        for (Integer item: itemToRecords.keySet()){
            for(Integer user: itemToRecords.get(item).getKey().keySet()){
                int rating = itemToRecords.get(item).getKey().get(user);
                PriorityQueue<Pair<Integer, Double>> neighbours = this.getNeighbours(item, sim, indexes, k, metric_type, range_type);

                Pair<Integer, Double> p = neighbours.peek();

//                double s = sim[getIndex(Math.min(p.getKey(), item), Math.max(p.getKey(), item), indexes)];
//                System.out.println("Compare: " + s + " - " + p.getValue());


                int prediction = prediction2(user, item, neighbours, cold_type);
                rmse += Math.pow(prediction - rating, 2);



                count += 1;

                if(prediction==rating){
                    number_exact += 1;
                }

                //System.out.println("Run " + count + " / " + total );

//                System.out.println(count + " - Added " + user + " " + item + " " + rating +  " P: " + prediction);
                if(count == total){
                    double mse = rmse / count;
                    return new Pair<>(mse, new Double(number_exact));

                }

            }
        }
        return null;
    }


    public int getPrediction(int user, int item, double[] sim, int[] indexes, int k, int metric_type, int cold_type, int range_type){
        PriorityQueue<Pair<Integer, Double>> neighbours = this.getNeighbours(item, sim, indexes, k, metric_type, range_type);
        int prediction = prediction2(user, item, neighbours, cold_type);
        return prediction;
    }

    public int getAveragePrediction(int user, int item){
        double avg = itemAverage(item, 1);
        double uAvg = findAverage2(user);
        return ((int) Math.round(((avg) + uAvg)/2));
    }

    /**
     * Compare two recomender sytems to benchmark systems
     * @param sim
     * @param indexes
     * @param total
     * @return
     */
    public List<Pair<Double, Double>> compare_recomender_systems(double[] sim, int[] indexes, int total){
        int count = 0;

        // accurate, fast, average, random

        double rmseAccurate = 0;
        double rmseFast = 0;
        double rmseAverge = 0;
        double rmseRandom = 0;

        int numberExactAccurate = 0;
        int numberExactFast = 0;
        int numberExactRandom = 0;
        int numberExactAverage = 0;

        for (Integer item: itemToRecords.keySet()){
            for(Integer user: itemToRecords.get(item).getKey().keySet()){
                int rating = itemToRecords.get(item).getKey().get(user);

//                double s = sim[getIndex(Math.min(p.getKey(), item), Math.max(p.getKey(), item), indexes)];
//                System.out.println("Compare: " + s + " - " + p.getValue());

                int predictionAccurate = getPrediction(user, item, sim, indexes, 19999, 0, 0, 0);
                int predictionFast = getPrediction(user, item, sim, indexes, 40, 1, 2, 0);
                int predictionRandom = new Random().nextInt(6);
                int predictionAverage = getAveragePrediction(user, item);


                rmseAccurate += Math.pow(predictionAccurate - rating, 2);
                rmseFast += Math.pow(predictionFast - rating, 2);
                rmseRandom += Math.pow(predictionRandom - rating, 2);
                rmseAverge += Math.pow(predictionAverage - rating, 2);

                count += 1;
                numberExactFast += ( predictionFast == rating ? 1 : 0);
                numberExactAccurate += ( predictionAccurate == rating ? 1 : 0);
                numberExactRandom += ( predictionRandom == rating ? 1 : 0);
                numberExactAverage += ( predictionAverage == rating ? 1 : 0);
                System.out.println("Run " + count + " / " + total );

//                System.out.println(count + " - Added " + user + " " + item + " " + rating +  " P: " + prediction);
                if(count == total){
                    List<Pair<Double, Double>> pairs = new ArrayList<>();
                    pairs.add(new Pair<>(rmseAccurate/count, new Double(numberExactAccurate)));
                    pairs.add(new Pair<>(rmseAverge/count, new Double(numberExactAverage)));
                    pairs.add(new Pair<>(rmseFast/count, new Double(numberExactFast)));
                    pairs.add(new Pair<>(rmseRandom/count, new Double(numberExactRandom)));
                    return pairs;

                }

            }
        }
        return null;
    }




    public Recommender(){
        similarityMatrix = new HashMap<>();
        records = new ArrayList<>();
    }

    public void retrieveAllItems2(){
        this.userToRecords = new HashMap<>();
        this.itemToRecords = new HashMap<>();


        long time = System.currentTimeMillis();
        try(Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD)){

            String sql = "SELECT * FROM socialdb.straining";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet =  preparedStatement.executeQuery();
            long timeElapsed = System.currentTimeMillis() - time;
            System.out.println("Database retrieval: " + timeElapsed);

            System.out.println("Finished retrieving data from database.");

            List<ItemRecord> records = new ArrayList<>();

            while (resultSet.next()) {
                int rating = resultSet.getInt("rating");
                int user = resultSet.getInt("userID");
                int itemID = resultSet.getInt("itemID");
                int ratingDate = resultSet.getInt("ratingDate");
                ItemRecord record = new ItemRecord(itemID, rating, user);

                Pair<List<ItemRecord>,DoubleValue> result = userToRecords.get(user);
                if (result == null){
                    result = new Pair<List<ItemRecord>,DoubleValue>(new ArrayList<>(),new DoubleValue(0.0));
                    userToRecords.put(user, result);
                }

                result.getKey().add(record);
                result.getValue().incrementValue(record.average);

                Pair<HashMap<Integer, Integer>, Pair<DoubleValue, DoubleValue>> value = itemToRecords.get(itemID);
                if(value == null){
                    value = new Pair<>(new HashMap<>(),new Pair<>(new DoubleValue(1000000000),
                            new DoubleValue(0)));
                    itemToRecords.put(itemID, value);
                }

                Pair<DoubleValue, DoubleValue> old = value.getValue();
                old.getKey().setValue( Math.min( old.getKey().getValue(), ratingDate) );
                old.getValue().setValue( Math.max( old.getKey().getValue(), ratingDate) );
                value.getKey().put(user, rating);

            }

            System.out.println("Finished loading data to hashmaps " +  userToRecords.get(1).getKey().size() );

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public double findAverage2(int userID){
        Pair<List<ItemRecord>, DoubleValue> values = userToRecords.get(userID);
        if(values == null){
            return 2.5;
        }
        return values.getValue().getValue() / values.getKey().size();
    }

    public double calculateSimilarity3(int item1, int item2) {

        if (itemToRecords.get(item1) == null || itemToRecords.get(item2) == null){
            return 0;
        }

        // By this point, both item1 and item2 have been rated by at least 1 user

        double similarityNum = 0;
        double item1Squared = 0;
        double item2Squared = 0;

        Set<Integer> set1 = itemToRecords.get(item1).getKey().keySet();
        Set<Integer> set2 = itemToRecords.get(item2).getKey().keySet();

        HashSet<Integer> both = new HashSet<>(set1);
        both.retainAll(set2);

        if (both.isEmpty()){
            return 0;
        }

        double userAverage;
        double item1Rating;
        double item2Rating;

        for(Integer user : both) {

            userAverage = findAverage2(user);
            item1Rating = itemToRecords.get(item1).getKey().get(user) - userAverage;
            item2Rating = itemToRecords.get(item2).getKey().get(user) - userAverage;

            similarityNum += item1Rating * item2Rating;
            item1Squared += item1Rating * item1Rating;
            item2Squared += item2Rating * item2Rating;

        }

        return similarityNum / ( Math.sqrt(item1Squared) * Math.sqrt(item2Squared) );

    }

    public static int getIndex(int x, int y, int[] index){
        return ( (x-2) > -1 ? index[x-2] : 0) + ( y - x - 1);
    }


    public static double[] getSim() throws Exception{
        String file1 = "/Users/faithnyota1/trainingdata/sim.ser";
        String file2 = "/Users/faithnyota1/trainingdata/index.ser";
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file1));
        double[] array = (double[]) in.readObject();
        in.close();
        return array;
    }

    public static int[] getIndex() throws Exception{
        String file1 = "/Users/faithnyota1/trainingdata/index.ser";
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file1));
        int[] array = (int[]) in.readObject();
        in.close();
        return array;
    }

    public static void testSavingArrays() throws Exception{
        double[] sim = new double[199970001];
        sim[900] = 6.9;
        int[] index = new int[20000];
        index[3000] = 21;
        saveArrays(sim, index);
    }


    public void saveSimilarityMatrix(int max) throws Exception{
        double[] sim = new double[199970001];
        // primitive arrays of exact size required chosen to save space
        int[] index = new int[20000];
        int ind = 0;
        int count =0;
        for(int item1=1; item1<=max; item1++) {
            for (int item2 = item1 + 1; item2 <= max; item2++) {
                // symmetrical so: item1 > item2 <= max
                sim[count] = this.calculateSimilarity3(item1, item2);
                // sim calculated using general adjusted cosine similarity formula
                count += 1;
            }
            // index(item) = starting position of item in sim array
            index[ind] = count;
            ind += 1;
        }
        // code below saves sim and index to secondary storage
        saveArrays(sim, index);
    }


    public static void main(String[] args) throws Exception{
        Recommender recommender = new Recommender();

        long time = System.currentTimeMillis();
        recommender.retrieveAllItems2();
        System.out.println("Finished all retrieval");

        // Getting similarity matrix and index
        double[] arr = getSim();
        int[] index = getIndex();
        System.out.println("Finished getting sim matrix");

        Integer[] kValues = {19999, 17500, 15000, 12500, 10000, 3000, 1500, 100, 750, 250, 10, 20, 80, 40, 30, 60, 90};
        List<Integer> ks = new ArrayList<>();
        for(Integer k: kValues)
            ks.add(k);

        /*
        //Running the experiment to find best system
        recommender.runExperiment(ks, 5000, arr, index);
        */

        // Comparing the system to benchmark ones
        List<Pair<Double, Double>> pairs = recommender.compare_recomender_systems(arr, index, 5000);
        String[] names = {"Accurate", "Average", "Fast", "Random" };
        for(int i=0; i<pairs.size(); i++){
            Pair p = pairs.get(i);
            String name = names[i];
            System.out.println(name + ": MSE="+p.getKey() + ", ExactMatch: " + p.getValue());
        }



    }


}
