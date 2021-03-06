package application;
	
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import com.fazecast.jSerialComm.*;


public class OsfControllerSim extends Application implements Initializable, SerialPortDataListener {
	private static final byte LCD_MSG_ID = 0x59;
	private static final byte CT_MSG_ID = 0x43;
	private static final int CT_OS_MSG_BYTES = 29;
	private static final int  LCD_OS_MSG_BYTES = 13;

	@FXML
	private Button startButton;
	@FXML
	private ChoiceBox<String> port;
	
    @FXML
    private Slider fw;	
	@FXML
	private Slider volts;
	@FXML
	private Slider current;
	@FXML
	private Slider speed;
	@FXML
	private CheckBox brake;
	@FXML
	private Slider adcValue;
	@FXML
	private Slider torqueADC;
	@FXML
	private Slider cadence;
	@FXML
	private Slider dutyCycle;
	@FXML
	private Slider erps;
	@FXML
	private Slider foc;
	@FXML
	private ChoiceBox<String> state;
	@FXML
	private Slider temperature;
	@FXML
	private Slider pedalTorque;
	@FXML
	private Slider dummy;
	
	@FXML
	private Label ridingMode;
	@FXML
	private Label lights;
	@FXML
	private Label voltCutOff;
	@FXML
	private Label maxSpeed;
	@FXML
	private Label whelPerimeter;
	@FXML
	private Label cruiseMode;
	@FXML
	private Label adcOption;
	@FXML
	private Label motorType;
	@FXML
	private Label minTemperature;
	@FXML
	private Label maxTemperature;
	@FXML
	private Label lightCfg;
	@FXML
	private Label assistWORotation;
	@FXML
	private Label motorAccel;
	@FXML
	private Label torqueADCStep;
	@FXML
	private Label maxCurrent;
	@FXML
	private Label maxPower;
	@FXML
	private Label motorAngleAdj;	
	@FXML
	private Label torqueFix;
	@FXML
	private Label phaseAngles;
	
	
	@FXML
	private TextArea error;
	
    private SerialPort serialPort = null;
    
    private ObservableList<String> portNames;
    private ObservableList<String> errors;

    private static final String OK = "OK";
    private static final String MOTORBLOCKED = "Motor Blocked";
    private static final String ERRTORQUE = "Err. Tor. Sensor";
    private static final String ERRCAD = "Err. Cad. Calib.";
    
    
    private static final String[] values = {OK,MOTORBLOCKED,ERRTORQUE,ERRCAD};
    private static final String[] modes = {"OFF","POVER","TORQUE","CADENCE","EMTB","WALK","CRUISE","CALIB","MTEST"};
    
    private double odo = 0;
    private double revs = 0;
    private byte ridingModeVal = 0;
    private int  ridingParamVal = 0;
    
    Timer timer = new Timer(); 
    TimerTask task = new MyTask(); 
	
    @Override
    public void initialize(URL location, ResourceBundle resources) {
	    String[] list = new String[SerialPort.getCommPorts().length];
	    int i=0;
	    for (SerialPort p: SerialPort.getCommPorts()) {
	    	list[i++]= p.getSystemPortName();
	    }
	    portNames = FXCollections.observableArrayList(list);
        port.setItems(portNames);
        if (!portNames.isEmpty()) {
            port.setValue(portNames.get(0));
        }
        if (port.getValue() == null) {
        	startButton.setDisable(true);
        }
        errors = FXCollections.observableArrayList(values);
        state.setItems(errors);
        state.setValue(errors.get(0));
    }
    
	
	@Override
	public void start(Stage primaryStage) {
		try {
	        primaryStage.setTitle("OSF Controller Simulator");
	        URL url = ClassLoader.getSystemResource("osfControllerSim.fxml");
	        if (url == null) {
	        	System.out.println("osfControllerSim.fxml NOT Found!!");
	        	System.exit(-1);
	        }
	        Parent root = FXMLLoader.load(url);
	        Scene scene = new Scene(root, 800, 700);
	        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
	        primaryStage.setScene(scene);
	        primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
    public void serialConnect() {
        if (serialPort == null || !serialPort.isOpen()) {
            odo = 0;
            if (openSerialPort()) {
               startButton.setText("Stop");
               timer = new Timer(); 
               task = new MyTask();
               timer.schedule(task, 100, 100);  // execute every 100 msec.
            }
        } else {
            timer.cancel();
            startButton.setText("Start");
            serialCloseAction();
        }
    }
    
    private boolean openSerialPort() {
    	boolean done = false;
    	try {
	    	serialPort = SerialPort.getCommPort((String) port.getValue());
	    	serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
	    	if (!serialPort.openPort())
	    		javafx.application.Platform.runLater( () -> error.appendText("Unable to open port " + (String)port.getValue() + "\n") );
	    	else {
	    		//serialPort.setComPortTimeouts( SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
	    		serialPort.addDataListener(this);
		        port.setDisable(true);
		        done = true;
	    		javafx.application.Platform.runLater( () -> error.appendText("Port opened: " + (String)port.getValue() + "\n") );
		        //int rt = serialPort.getReadTimeout();
		        //int wt = serialPort.getWriteTimeout();
	    		//javafx.application.Platform.runLater( () -> error.appendText("Read Timeout:" + rt + " - Write Timeout:" + wt + "\n") );
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    		javafx.application.Platform.runLater( () -> error.appendText("Unable to create port " + (String) port.getValue() + "\n") );
    	}
    	return done;
    }

    private void closeSerialPort() {
        if (serialPort != null && serialPort.isOpen()) {
    		serialPort.removeDataListener();
        	if (!serialPort.closePort())
        		javafx.application.Platform.runLater( () -> error.appendText("Unable to close port " + serialPort.getSystemPortName() + "\n") );
        }
        serialPort = null;
    }

    public void serialCloseAction() {
        closeSerialPort();
        port.setDisable(false);
    }
    
    public void updateGUI(byte[] data) {
    	String str;
    	int val;
    	ridingModeVal = data[2];
    	ridingParamVal = (data[3]&0xff);
    	str = modes[ridingModeVal] + " - " + String.valueOf(ridingParamVal);
    	this.ridingMode.setText(str);
    	this.lights.setText(data[4]>0?"ON":"OFF");
    	switch (data[1]) {
    	case 0:
    		val = unsignedByteToInt(data[5]);
            val += unsignedByteToInt(data[6]) << 8;
        	this.voltCutOff.setText(String.valueOf(val));
        	this.maxCurrent.setText(String.valueOf(data[7]));
        	this.maxPower.setText(String.valueOf(data[8]));
        	this.motorType.setText(String.valueOf(unsignedByteToInt(data[9])));
        	this.motorAccel.setText(String.valueOf(data[10]));
        	break;
    	case 1:
        	this.motorAngleAdj.setText(String.valueOf(data[5]&0xff));
    		val = unsignedByteToInt(data[6]);
            val += unsignedByteToInt(data[7]) << 8;
        	this.whelPerimeter.setText(String.valueOf(val));
        	this.maxSpeed.setText(String.valueOf(data[8]));
        	this.assistWORotation.setText(String.valueOf(data[9]));
        	this.lightCfg.setText(String.valueOf(data[10]));
    		break;
    	case 2:
        	this.adcOption.setText(String.valueOf(data[5]));
        	this.minTemperature.setText(String.valueOf(unsignedByteToInt(data[6])));
        	this.maxTemperature.setText(String.valueOf(unsignedByteToInt(data[7])));
        	val = unsignedByteToInt(data[8]);
            val += unsignedByteToInt(data[9]) << 8;
            if ((val & 0x8000) != 0)
            	this.torqueFix.setText(String.valueOf(val&0x7fff));
            else
            	this.torqueFix.setText(String.valueOf(0));
        	this.torqueADCStep.setText(String.valueOf(data[10]));
    		break;
    	case 3:
        	this.phaseAngles.setText(String.format(Locale.getDefault(), "%d,%d,%d,%d,%d,%d", 
        			unsignedByteToInt(data[5]),unsignedByteToInt(data[6]),unsignedByteToInt(data[7]),
        			unsignedByteToInt(data[8]),unsignedByteToInt(data[9]),unsignedByteToInt(data[10])));
    		break;
    	case 4:
        	// TODO ui8_hall_counter_offset_cal
    		break;
    	}
    }
    
    class MyTask extends TimerTask { 
    	int i;
    	public void run() {
    		
    	    odo +=  OsfControllerSim.this.speed.getValue() * 1000. / 36.; // distanza percorsa in mm
    	    revs += OsfControllerSim.this.cadence.getValue() / 60 * 0.1; // numero rivoluziioni pedali
    	    long wheelRev = (long)odo/2300;
            long crankRev = (long)revs;
    	    
    		byte[] msg = new byte[CT_OS_MSG_BYTES];

    		int val;
    		
    		msg[0] = CT_MSG_ID;
    		
    		// battery Volt
    		val = (int)(OsfControllerSim.this.volts.getValue() * 1000);
    		msg[1] = (byte)(val&0xff);
    		msg[2] = (byte)((val>>8) & 0xff);
    		
    		// Current
    		msg[3] = (byte)((int)(current.getValue()*10) & 0xff);
    		
    		// wheel speed
    		val = (int)(OsfControllerSim.this.speed.getValue() * 10);
    		msg[4] = (byte)(val&0xff);
    		msg[5] = (byte)((val>>8) & 0xff);
    		
    		// pedal cadence
    		msg[6] = (byte)(OsfControllerSim.this.cadence.getValue());
    		
    		// brake state & FW Version
    		msg[7] = (byte)(OsfControllerSim.this.brake.isSelected()?1:0);
    		val = (int)OsfControllerSim.this.fw.getValue();
    		msg[7] |= (byte)((val<<1) & 0xfe);
    		
    		// controller system state
    		switch(OsfControllerSim.this.state.getValue()) {
    		case OK:
    			msg[8] = 0;
    			break;
    		case MOTORBLOCKED:
    			msg[8] = 1;
    			break;
    		case ERRTORQUE:
    			msg[8] = 2;
    			break;
    		case ERRCAD:
    			msg[8] = 7;
    			break;
    		}

    		// motor temperature
    		msg[9] = (byte)(OsfControllerSim.this.temperature.getValue());
    		
    		if (ridingModeVal == 7) {
    			val = (int)(57D/360D*250000D/(double)ridingParamVal + 20);
    			msg[10] = (byte)(val & 0xff);
    			msg[11] = (byte)((val >> 8) & 0xff);
    			val = (int)(58D/360D*250000D/(double)ridingParamVal - 20);
    			msg[12] = (byte)(val & 0xff);
    			msg[13] = (byte)((val >> 8) & 0xff);
    			val = (int)(59D/360D*250000D/(double)ridingParamVal + 20);
    			msg[14] = (byte)(val & 0xff);
    			msg[15] = (byte)((val >> 8) & 0xff);
    			val = (int)(61D/360D*250000D/(double)ridingParamVal - 20);
    			msg[16] = (byte)(val & 0xff);
    			msg[17] = (byte)((val >> 8) & 0xff);
    			val = (int)(62D/360D*250000D/(double)ridingParamVal + 20);
    			msg[18] = (byte)(val & 0xff);
    			msg[19] = (byte)((val >> 8) & 0xff);
    			val = (int)(63D/360D*250000D/(double)ridingParamVal - 20);
    			msg[20] = (byte)(val & 0xff);
    			msg[21] = (byte)((val >> 8) & 0xff);
    		} else {
	    		// value from optional ADC channel
	    		msg[10] = (byte)(OsfControllerSim.this.adcValue.getValue());
	    		msg[11] = (byte)(OsfControllerSim.this.adcValue.getValue());
	    		
	    		// ADC pedal torque
	    		val = (int)(OsfControllerSim.this.torqueADC.getValue());
	    		msg[12] = (byte)(val&0xff);
	    		msg[13] = (byte)((val>>8) & 0xff);
	    		
	    		// PWM duty_cycle
	    		msg[14] = (byte)(OsfControllerSim.this.dutyCycle.getValue());
	    		
	    		// motor speed in ERPS
	    		val = (int)(OsfControllerSim.this.erps.getValue());
	    		msg[15] = (byte)(val&0xff);
	    		msg[16] = (byte)((val>>8) & 0xff);
	    		
	    		// FOC angle
	    		msg[17] = (byte)(OsfControllerSim.this.foc.getValue());
	    		
	            // wheel_speed_sensor_tick_counter
	    		msg[18] = (byte) (wheelRev & 0xff);
	    		msg[19] = (byte) ((wheelRev >> 8) & 0xff);
	    		msg[20] = (byte) ((wheelRev >> 16) & 0xff);
	    		
	    		// pedal torque x100
	    		val = (int)(OsfControllerSim.this.pedalTorque.getValue()*100);
	    		msg[21] = (byte)(val&0xff);
	    		msg[22] = (byte)((val>>8) & 0xff);
	            
	            // Crank revolutions
	            msg[23] = (byte)(crankRev&0xff);
	            msg[24] = (byte)((crankRev>>8) & 0xff);
	            
	    		// Dummy
	    		val = (int)(OsfControllerSim.this.dummy.getValue());
	    		msg[25] = (byte)(val&0xff);
	    		msg[26] = (byte)((val>>8) & 0xff);
    		}

    		// Calcolo CRC
    		int crc_tx = 0xffff;
            for (int i = 0; i < CT_OS_MSG_BYTES-2; i++)
            	crc_tx = crc16 (msg[i], crc_tx);
            msg[CT_OS_MSG_BYTES-2] = (byte) (crc_tx & 0xff);
            msg[CT_OS_MSG_BYTES-1] = (byte) ((crc_tx >> 8) & 0xff);	

     	   //javafx.application.Platform.runLater( () -> error.appendText("Sending " + bytesToHex(msg, crc_tx) + " bytes\n"));
      	   serialPort.writeBytes(msg, CT_OS_MSG_BYTES);
     	   
    	   //javafx.application.Platform.runLater( () -> error.appendText("Sent " + ret + " bytes\n"));
       } 
    } 
    
    
    @Override
	public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }

	/*
	@Override
	public byte[] getMessageDelimiter() {return new byte[] {ETX};}

	@Override
	public boolean delimiterIndicatesEndOfMessage() {return true;}
	*/
	
	int listen_state = 0;
	int rx_counter = 0;
	byte[] rx_msg = new byte[1024];
	byte[] b = new byte[1];
	@Override
	public void serialEvent(SerialPortEvent event)
	{
	  if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
		  return;
	  while (serialPort.bytesAvailable() > 0) {
		  serialPort.readBytes(b, 1);
		  switch(listen_state) {
	    	  case 0:
	    		  if (b[0] == LCD_MSG_ID) {
	    			  rx_msg[0] = b[0];
	    			  listen_state = 1;
	    			  rx_counter = 1;
	    		  }
	    		  break;
	    	  case 1:
	    		  rx_msg[rx_counter++] = b[0];
	    		  if (rx_counter >= LCD_OS_MSG_BYTES) {
	    			  listen_state = 0;
	    			  rx_counter = 0;
					  if (checkCRC(rx_msg)) {
						  final byte[] data = rx_msg.clone();
						  javafx.application.Platform.runLater( () -> updateGUI(data) );
					  } else {
						  final String s1 = bytesToHex(rx_msg, LCD_OS_MSG_BYTES);
						  javafx.application.Platform.runLater( () -> error.appendText("CRC Error: " + s1 + "\n") );
					  }
					}
					break;
		  }
	  }
	}
	
	boolean checkCRC(byte[] msg) {
		int crc_calc = 0xffff;
        for (int i = 0; i < LCD_OS_MSG_BYTES-2; i++)
        	crc_calc = crc16(msg[i], crc_calc);
        int crc_rx = (msg[LCD_OS_MSG_BYTES-2] & 255) + ((msg[LCD_OS_MSG_BYTES-1] & 255) << 8);
        return crc_rx == crc_calc;
	}
	
	// return true if the crc8 of the message is correct
    int crc16(byte ui8_data, int ui16_crc)
    {
      int i;

      ui16_crc ^= (ui8_data & 0xff);

      for (i = 8; i > 0; i--)
      {
        if ((ui16_crc & 0x0001) == 1) { ui16_crc = (ui16_crc >>> 1) ^ 0xA001; }
        else { ui16_crc >>>= 1; }
      }
      return ui16_crc & 0xffff;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, int n) {
    	StringBuilder sb = new StringBuilder();
        for (int j = 0; j < n; j++) {
            int v = bytes[j] & 0xFF;
            sb.append(HEX_ARRAY[v >>> 4]);
            sb.append(HEX_ARRAY[v & 0x0F]);
            sb.append(" ");
        }
        return sb.toString();
    }
    
    private int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }
}

