package org.lappsgrid.example;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

/**
 * ElementAnnotation: parse question and answers from the input file
 * @author yuqizhang
 * 
 */
public class ElementAnnotation implements ProcessingService{
  /**
   * The Json String required by getMetadata()
   */
  private String metadata;


  public ElementAnnotation() {
      metadata = generateMetadata();
  }
  
  /**
   * Generate Metadata
   * Input format: TEXT/LAPPS
   * Output format: LAPPS
   * Annotation format: Uri.SENTENCE
   * @return
   */
  private String generateMetadata() {
      // Create and populate the metadata object
      ServiceMetadata metadata = new ServiceMetadata();

      // Populate metadata using setX() methods
      metadata.setName(this.getClass().getName());
      metadata.setDescription("Element Annotation");
      metadata.setVersion("1.0.0-SNAPSHOT");
      metadata.setVendor("http://www.lappsgrid.org");
      metadata.setLicense(Uri.APACHE2);

      // JSON for input information
      IOSpecification requires = new IOSpecification();
      requires.addFormat(Uri.TEXT);           // Plain text (form)
      requires.addFormat(Uri.LIF);            // LIF (form)
      requires.addLanguage("en");             // Source language
      requires.setEncoding("UTF-8");

      // JSON for output information
      IOSpecification produces = new IOSpecification();
      produces.addFormat(Uri.LAPPS);          // LIF (form) synonymous to LIF
      produces.addAnnotation(Uri.SENTENCE);      // SENTENCE (contents)
      requires.addLanguage("en");             // Target language
      produces.setEncoding("UTF-8");

      // Embed I/O metadata JSON objects
      metadata.setRequires(requires);
      metadata.setProduces(produces);

      // Serialize the metadata to a string and return
      Data<ServiceMetadata> data = new Data<ServiceMetadata>(Uri.META, metadata);
      return data.asPrettyJson();
  }

  @Override
  /**
   * getMetadata simply returns metadata populated in the constructor
   */
  public String getMetadata() {
      return metadata;
  }

  @Override
  /**
   * Parse the document and annotate the question and answers
   */
  public String execute(String input) {
      // Step #1: Parse the input.
      Data data = Serializer.parse(input, Data.class);

      // Step #2: Check the discriminator
      final String discriminator = data.getDiscriminator();
      if (discriminator.equals(Uri.ERROR)) {
          // Return the input unchanged.
          return input;
      }

      // Step #3: Extract the text.
      Container container = null;
      if (discriminator.equals(Uri.TEXT)) {
          container = new Container();
          container.setText(data.getPayload().toString());
      }
      else if (discriminator.equals(Uri.LAPPS)) {
          container = new Container((Map) data.getPayload());
      }
      else {
          // This is a format we don't accept.
          String message = String.format("Unsupported discriminator type: %s", discriminator);
          return new Data<String>(Uri.ERROR, message).asJson();
      }

      // Step #4: Create a new View
      View view = container.newView();

      
      String text = container.getText();
      int id = 0;
      try {
        //BufferedReader br = new BufferedReader(new FileReader(text));
        String[] lines = text.split("\n");
        // String qline = null;
        for (String qline:lines){
          
          //System.out.println(qline);
          int start = qline.indexOf(" ");
          int end = qline.length();
          if(qline.substring(0, start).equals("Q")){
            Annotation a = view.newAnnotation("Question", Uri.SENTENCE, start, end);
            a.addFeature("Type", "Question");
            a.addFeature(Uri.SENTENCE, qline.substring(start+1));
          }
          else{
            int newstart = qline.indexOf(" ", start+1);
            String score = qline.substring(start+1, newstart);
            Annotation a = view.newAnnotation("A" + (++id), Uri.SENTENCE, newstart, end);
            a.addFeature("Type", "Answer");
            a.addFeature(Uri.SENTENCE, qline.substring(newstart+1));
            a.addFeature("Score", score);
          }
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
  
      
      
   
      

      // Step #6: Update the view's metadata. Each view contains metadata about the
      // annotations it contains, in particular the name of the tool that produced the
      // annotations.
      view.addContains(Uri.SENTENCE, this.getClass().getName(), "elementAnnotation");

      // Step #7: Create a DataContainer with the result.
      data = new DataContainer(container);

      // Step #8: Serialize the data object and return the JSON.
      return data.asPrettyJson();
  }
}
