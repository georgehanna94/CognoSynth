package com.example.fftsample;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;

/**
 * Created by George on 2017-03-04.
 */

public class CSV2Arff {
    /**
     * takes 2 arguments:
     * - CSV input file
     * - ARFF output file
     */
    private String file_path,newfile_path;
    private File file,newfile;

    public CSV2Arff(){
        File root = Environment.getExternalStorageDirectory();
        this.file_path = root.getAbsolutePath()+ "/FFTSample/bandpowerValue.csv";
        this.file =new File(file_path);
        this.newfile_path = root.getAbsolutePath()+ "/FFTSample/bandpowerValue.arff";
        this.newfile =new File(newfile_path);


    }

    public void converter() {
        // load CSV
        CSVLoader loader = new CSVLoader();

        try {
            loader.setSource(file);
            Instances data = loader.getDataSet();
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(newfile);
            saver.setDestination(newfile);
            saver.writeBatch();

        }catch (IOException e) {
        //	// TODO Auto-generated catch block
        e.printStackTrace();
        }
        // save ARFF

    }
}
