package edu.dhbw.andobjviewer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Date;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;
import edu.dhbw.andar.ARToolkit;
import edu.dhbw.andar.AndARActivity;
import edu.dhbw.andar.exceptions.AndARException;
import edu.dhbw.andarmodelviewer.R;
import edu.dhbw.andobjviewer.graphics.LightingRenderer;
import edu.dhbw.andobjviewer.graphics.Model3D;
import edu.dhbw.andobjviewer.models.Model;
import edu.dhbw.andobjviewer.parser.ObjParser;
import edu.dhbw.andobjviewer.parser.ParseException;
import edu.dhbw.andobjviewer.parser.Util;
import edu.dhbw.andobjviewer.util.AssetsFileUtil;
import edu.dhbw.andobjviewer.util.BaseFileUtil;
import edu.dhbw.andobjviewer.util.SDCardFileUtil;

/**
 * Example of an application that makes use of the AndAR toolkit.
 * 
 * @author Tobi
 * 
 */
public class AugmentedModelViewerActivity extends AndARActivity implements SurfaceHolder.Callback
{

    /**
     * View a file in the assets folder
     */
    public static final int TYPE_INTERNAL = 0;

    /**
     * View a file on the sd card.
     */
    public static final int TYPE_EXTERNAL = 1;

    public static final boolean DEBUG = false;

    /* Menu Options: */
    private final int MENU_SCALE = 0;

    private final int MENU_ROTATE = 1;

    private final int MENU_TRANSLATE = 2;

    private final int MENU_SCREENSHOT = 3;

    private int mode = MENU_SCALE;

    private Model model;

    private Model3D model3d;

    private ProgressDialog waitDialog;

    private Resources res;

    ARToolkit artoolkit;

    public AugmentedModelViewerActivity()
    {

	super(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

	super.onCreate(savedInstanceState);
	super.setNonARRenderer(new LightingRenderer());// or might be omited
	res = getResources();
	artoolkit = getArtoolkit();

	getSurfaceView().getHolder().addCallback(this);
    }

    /**
     * Inform the user about exceptions that occurred in background threads.
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {

	System.out.println("");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {

	super.surfaceCreated(holder);
	// load the model
	// this is done here, to assure the surface was already created, so that
	// the preview can be started
	// after loading the model
	if (model == null)
	{
	    waitDialog = ProgressDialog.show(this, "", getResources().getText(R.string.loading), true);
	    waitDialog.show();
	    new ModelLoader().execute();
	}
    }

    /**
     * Handles touch events.
     * 
     * @author Tobias Domhan
     * 
     */

    private class ModelLoader extends AsyncTask<Void, Void, Void>
    {

	@Override
	protected Void doInBackground(Void... params)
	{

	    Intent intent = getIntent();
	    Bundle data = intent.getExtras();
	    int type = data.getInt("type");
	    String modelFileName = data.getString("name");
	    BaseFileUtil fileUtil = null;
	    File modelFile = null;
	    switch (type)
	    {
		case TYPE_EXTERNAL:
		    fileUtil = new SDCardFileUtil();
		    modelFile = new File(URI.create(modelFileName));
		    modelFileName = modelFile.getName();
		    fileUtil.setBaseFolder(modelFile.getParentFile().getAbsolutePath());
		    break;
		case TYPE_INTERNAL:
		    fileUtil = new AssetsFileUtil(getResources().getAssets());
		    fileUtil.setBaseFolder("models/");
		    break;
	    }

	    // read the model file:
	    if (modelFileName.endsWith(".obj"))
	    {
		ObjParser parser = new ObjParser(fileUtil);
		try
		{
		    if (Config.DEBUG)
			Debug.startMethodTracing("AndObjViewer");
		    if (type == TYPE_EXTERNAL)
		    {
			// an external file might be trimmed
			BufferedReader modelFileReader = new BufferedReader(new FileReader(modelFile));
			String shebang = modelFileReader.readLine();
			if (!shebang.equals("#trimmed"))
			{
			    // trim the file:
			    File trimmedFile = new File(modelFile.getAbsolutePath() + ".tmp");
			    BufferedWriter trimmedFileWriter = new BufferedWriter(new FileWriter(trimmedFile));
			    Util.trim(modelFileReader, trimmedFileWriter);
			    if (modelFile.delete())
			    {
				trimmedFile.renameTo(modelFile);
			    }
			}
		    }
		    if (fileUtil != null)
		    {
			BufferedReader fileReader = fileUtil.getReaderFromName(modelFileName);
			if (fileReader != null)
			{
			    model = parser.parse("Model", fileReader);
			    model3d = new Model3D(model);
			}
		    }
		    if (Config.DEBUG)
			Debug.stopMethodTracing();
		}
		catch (IOException e)
		{
		    e.printStackTrace();
		}
		catch (ParseException e)
		{
		    e.printStackTrace();
		}
	    }
	    return null;
	}

	@Override
	protected void onPostExecute(Void result)
	{

	    super.onPostExecute(result);
	    waitDialog.dismiss();

	    // register model
	    try
	    {
		if (model3d != null)
		    artoolkit.registerARObject(model3d);
	    }
	    catch (AndARException e)
	    {
		e.printStackTrace();
	    }

	    model.setScale(20);
	    // model.setXrot(50f);
	    model.setYrot(-75f);

	    startPreview();
	}
    }

}
