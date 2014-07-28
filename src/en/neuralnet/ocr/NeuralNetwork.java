package en.neuralnet.ocr;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import en.neuralnet.ocr.data.WeightManager;
/*
 * The NeuralNetwork main class retrieves the grayscale value maps for
 * each input image from the ImageManager class.
 * 
 * It initializes the neurons that each accept the map as their input; the neurons are 
 * trained to recognize configurations that most closely resemble their designated
 * output character through a backpropagation learning algorithm.
 * 
 * Authors: Greg Carlin, Pratap Singh, and Ethan Sargent
 * 
 */
public class NeuralNetwork {
	//Array of all possible outputs (guesses of the neural network)
	private static final char[] CHARS = "0123456789".toCharArray();
	
	//Final Learning Speed
	private static final double ETA = 0.2;
	
	// defines the hidden layers of the network. each number is the size of a hidden layer.
	private static final int[] HIDDEN_LAYERS = {300};
	
	// the number of pixels in the image
	private static final int IMAGE_SIZE = 28 * 28;
	
	private final WeightManager weightManager;
	private final Neuron[][] neurons = new Neuron[HIDDEN_LAYERS.length + 1][];
	
	public NeuralNetwork() {
		this.weightManager = new WeightManager(IMAGE_SIZE, HIDDEN_LAYERS, CHARS);
		
		for(int j=0; j<neurons.length; j++) {
			// add neurons to this layer. if this is the output layer, number of neurons is taken from CHARS constant.
			neurons[j] = new Neuron[j < HIDDEN_LAYERS.length ? HIDDEN_LAYERS[j] : CHARS.length];
			for(int k=0; k<neurons[j].length; k++) {
				neurons[j][k] = new Neuron(j < HIDDEN_LAYERS.length ? k : Character.getNumericValue(CHARS[k]));
				int neuronID = neurons[j][k].getID();
				neurons[j][k].setWeights(weightManager.getWeights(j, neuronID));
				neurons[j][k].setBias(weightManager.getBias(j, neuronID));
			}
		}
	}
	
	/**
	 * Propagates the input image through the network.
	 * 
	 * @param input The initial input image.
	 * @return The outputs of the last layer of the network.
	 */
	private double[] forwardPropagate(double[] input) {
		// for each layer i in the network
		double[] lastOutput = input;
		for(int i=0; i<neurons.length; i++) {
			// for each neuron j in the layer
			double[] thisOutput = new double[neurons[i].length];
			for(int j=0; j<neurons[i].length; j++) {
				double weightedSum = neurons[i][j].getOutput(lastOutput);
				thisOutput[j] = sigmoidFunction(weightedSum);
			}
			lastOutput = thisOutput;
		}
		return lastOutput;
	}
	
	public void train(double[] image, char answer) {
		double[] outputs = forwardPropagate(image);
		
		Neuron[] outputNeurons = neurons[neurons.length - 1];
		// for each neuron in the output layer
		for(int i=0; i<outputNeurons.length; i++) {
			Neuron neuron = outputNeurons[i];
			neuron.setDelta(sigmoidPrime(neuron.getWeightedSum()) * ((((char) neuron.getID()) == answer ? 1.0 : 0.0) - outputs[i]));
		}
		
		// going backwards, for each layer except the output layer
		for(int i=neurons.length-2; i>=0; i--) {
			// for each neuron in this layer
			for(int j=0; j<neurons[i].length; j++) {
				double weightDeltaSum = 0.0;
				// for each neuron in the layer after
				for(int k=0; k<neurons[i+1].length; k++) {
					weightDeltaSum += neurons[i+1][k].getDelta() * neurons[i+1][k].getWeights()[j];
				}
				neurons[i][j].setDelta(sigmoidPrime(neurons[i][j].getWeightedSum()) * weightDeltaSum);
			}
		}
		
		// for each layer in the network
		for(int i=0; i<neurons.length; i++) {
			// for each neuron in the layer
			for(int j=0; j<neurons[i].length; j++) {
				
				// update weights
				double[] weights = neurons[i][j].getWeights();
				// for each weight in the neuron
				for(int k=0; k<weights.length; k++) {
					weights[k] += ETA * sigmoidFunction(neurons[i][j].getInputs()[k]) * neurons[i][j].getDelta();
					System.out.println(ETA * sigmoidFunction(neurons[i][j].getInputs()[k]) * neurons[i][j].getDelta());
				}
				neurons[i][j].setWeights(weights);
				weightManager.setWeights(i, neurons[i][j].getID(), weights);
				
				// update bias
				double bias = neurons[i][j].getBias();
				bias += ETA * 1.0 * neurons[i][j].getDelta();
				neurons[i][j].setBias(bias);
				weightManager.setBias(i, neurons[i][j].getID(), bias);
				
			}
		}
	}
	
	public void saveWeights() {
		weightManager.save();
	}
	
	public char guess(double[] image) {
		double[] outputs = forwardPropagate(image);
		
		SortedMap<Double,Character> solutions = new TreeMap<Double,Character>();
		for (int j = 0; j < CHARS.length; j ++) {
			solutions.put(outputs[j], CHARS[j]);
		}
		
		Set<Entry<Double,Character>> solSet = solutions.entrySet();
		int j = 0;
		for (Entry<Double,Character> e : solSet) {
			if(j >= 5) System.out.println("\tGuess " + (CHARS.length - j) + ": " + e.getValue());
			j++;
		}
		
		return solutions.get(solutions.lastKey());
	}
	
	private static BufferedImage bufferAndScale(Image i, int side) {
	    //if(i instanceof BufferedImage) return (BufferedImage) i;
	    
	    BufferedImage bi = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = bi.createGraphics();
	    g.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
	    g.drawImage(i, 0, 0, side, side, null);
	    g.dispose();
	    
	    return bi;
	}

	private static double sigmoidPrime(double x) {
		double temp = sigmoidFunction(x);
		return temp * (1 - temp);
	}
	
	//The sigmoid function converts weighted sums into neuron outputs
	public static double sigmoidFunction(double in) {
		return (1/(1 + Math.exp(-in)));
	}
	
	/**
     * Used for debugging.
     * 
     * @param i
     * @param out
     */
    public static final void saveImage(BufferedImage i, String out) {
    	try {
    		//System.out.println("saving image with extension " + out.substring(out.lastIndexOf(".") + 1) + " to " + out);
			ImageIO.write(i, out.substring(out.lastIndexOf(".") + 1), new File(out));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
