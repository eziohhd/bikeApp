package com.example.biking;


import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Common {

    // private array to help with continious input data
    private double[] m_array;
    private double m_accDrift;

    public double[] calculateMovingAverage(double[] array, double n) {

        int num = array.length;
        int initial = (int) Math.ceil(n / 2);
        int lenChange = initial;
        double[] avgs = new double[num];
        double sum = 0;
        if (num <= initial) {
            double average = average(array);
            for (int i = 0; i < num; i++) {
                avgs[i] = average;
            }
        } else {
            // Calculate the average of the first array whose length is shorter than
            // ceil(n/2)
            for (int k = 0; k < (int) n - initial; ++k) {
                for (int i = 0; i < lenChange; ++i) {
                    sum += array[i];
                }
                avgs[k] = sum / lenChange;
                lenChange++;
                sum = 0;
            }
            // Moving average of the array whose length is n
            int startIndex = 0;
            int avgsIndex = (int) n - initial;
            while (startIndex < num - (int) n + 1) {
                double[] newArray = Arrays.copyOfRange(array, startIndex, startIndex + (int) n);
                avgs[avgsIndex] = average(newArray);
                avgsIndex++;
                startIndex++;
            }
            // Calculate the average of the last array whose length is shorter than
            // ceil(n/2)
            lenChange = (int) n - 1;
            for (int k = num - initial + 1; k < num; ++k) {
                for (int i = 0; i < lenChange; ++i) {
                    sum += array[num - i - 1];
                }
                avgs[k] = sum / lenChange;
                lenChange--;
                sum = 0;
            }

        }
        return avgs;

    }

    public static double average(double[] array) {
        double sum = 0.0;

        for (double num : array) {
            sum += num;
        }

        double average = sum / array.length;
        return average;
    }

    public double max(double[] array) {
        double max = 0;

        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    public double min(double[] array) {
        double min = array[0];

        for (int i = 0; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public void write(String filename, ArrayList<Double> x) throws IOException {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter(filename));
        for (int i = 0; i < x.size(); i++) {
            outputWriter.write(Double.toString(x.get(i)));
            outputWriter.newLine();
        }
        outputWriter.flush();
        outputWriter.close();
    }

    public double[] accCompensation(double[] inputData) {

        // store peaks and valleys
        ArrayList<Integer> peakValleyIndex = new ArrayList<Integer>();
        ArrayList<Double> peakValleys = new ArrayList<Double>();
        ArrayList<Integer> motionStatus = new ArrayList<Integer>();

        // array concatanation
        int m_arrayLen;
        if (m_array != null) {
            m_arrayLen = m_array.length;
        } else {
            m_arrayLen = 0;
        }

        double[] array = new double[inputData.length + m_arrayLen];

        if (m_array != null) {
            System.arraycopy(m_array, 0, array, 0, m_arrayLen);
        }
        System.arraycopy(inputData, 0, array, m_arrayLen, inputData.length);

        // m_array clear
        m_array = new double[0];

        // length of array
        int ArrayLength = array.length;

        // thresholds
        double threshold1 = 0.8;
        double threshold2 = 1.2;

        // find peaks and valleys
        int counterPeaks = 0;
        int counterValleys = 0;
        int counterThreshold = 100;
        peakValleyIndex.add(0);
        peakValleys.add(array[0]);
        for (int x = counterThreshold + 1; x < ArrayLength - counterThreshold; ++x) {
            for (int k = x - counterThreshold; k <= x + counterThreshold; ++k) {
                if (array[x] > array[k]) {
                    counterPeaks++;
                } else if (array[x] < array[k]) {
                    counterValleys++;
                }

                if ((counterPeaks > 2 * counterThreshold - 1) || (counterValleys > 2 * counterThreshold - 1)) {
                    peakValleyIndex.add(x);
                    // Store peaks and valleys
                    peakValleys.add(array[x]);
                }
            }
            // counter reset
            counterPeaks = 0;
            counterValleys = 0;

        }

        // find motion status based on the peaks and valleys
        for (int i = 0; i < peakValleyIndex.size() - 1; ++i) {
            if (((peakValleys.get(i + 1) - peakValleys.get(i)) > threshold1)&& (peakValleys.get(i + 1) > threshold1))
                motionStatus.add(1);// acc
            else if (((peakValleys.get(i + 1) - peakValleys.get(i)) < -threshold2) && (peakValleys.get(i) > threshold1))
                motionStatus.add(1);// acc
            else if (((peakValleys.get(i + 1) - peakValleys.get(i)) < -threshold1) &&(peakValleys.get(i + 1) < -threshold2))
                motionStatus.add(1);// dec
            else if (((peakValleys.get(i + 1) - peakValleys.get(i)) > threshold2) &&(peakValleys.get(i) < -threshold1))
                motionStatus.add(1);// dec
            else
                motionStatus.add(0);// const

        }

        // merge zones with the same motion status
        int k = 0;
        int len = motionStatus.size();
        while (k < motionStatus.size() - 1) {
            if (motionStatus.get(k) == motionStatus.get(k + 1)) {
                peakValleyIndex.remove(k + 1);
                motionStatus.remove(k + 1);
                len = len - 1;
                k = k - 1;
            }
            k = k + 1;
        }

        // acc compensation except for the last motion status zone
        double avgconstV = 0;
        for (int i = 0; i < motionStatus.size() - 1; ++i) {
            if ((motionStatus.get(i) == 1) && (i < motionStatus.size())) {
                double[] constV = Arrays.copyOfRange(array, peakValleyIndex.get(i + 1), peakValleyIndex.get(i + 2));
                avgconstV = average(constV);
                if (avgconstV < 0) {
                    for (int j = peakValleyIndex.get(i); j < peakValleyIndex.get(i + 2); ++j) {
                        array[j] -= avgconstV;
                    }
                }

            }

        }
        // check the last motion status zone and return accComp output
        if (motionStatus.size() > 1) {
            if ((motionStatus.get(motionStatus.size() - 1)) == 1) {
                int indexLastZoneStart = peakValleyIndex.get(peakValleyIndex.size() - 1);
                int LastZoneLen = array.length - indexLastZoneStart;
                m_array = new double[LastZoneLen];
                System.arraycopy(array, indexLastZoneStart, m_array, 0, LastZoneLen);
                double[] output = new double[array.length - LastZoneLen];
                System.arraycopy(array, 0, output, 0, array.length - LastZoneLen);

                // clear private variables
                return output;
            } else {

                return array;
            }
        } else if ((motionStatus.size() > 0) && (motionStatus.get(motionStatus.size() - 1) == 0)) {
            avgconstV = average(array);
            m_accDrift = avgconstV;
            if (avgconstV < 0) {
                for (int j = 0; j < array.length; ++j) {
                    array[j] -= avgconstV;
                }
            }
            return array;
        } else if ((motionStatus.size() > 0) && (motionStatus.get(motionStatus.size() - 1) == 1)) {

            // int LastZoneLen = array.length;
            // m_array = new double[LastZoneLen];
            // System.arraycopy(array, 0, m_array, 0, LastZoneLen);
            // return null;
            for(int i = 0;i < array.length;i++)
            {
                array[i] = array[i]*2;
            }
            return array;
        } else { // cannot detect motion status
            double driftAcc = min(array);
            avgconstV = average(array);
            if (avgconstV < 0) {
                for (int j = 0; j < array.length; ++j) {
                    array[j] -= avgconstV;
                }
            } else if(driftAcc<0){
                for (int j = 0; j < array.length; ++j) {
                    array[j] -= driftAcc;
                }
            }
            return array;
        }
    }

}
