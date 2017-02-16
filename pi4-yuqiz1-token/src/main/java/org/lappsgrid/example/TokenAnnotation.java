package org.lappsgrid.example;

import java.util.List;
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
 * TokenAnnotation: tokenize question and answers
 * @author yuqizhang
 *
 */
public class TokenAnnotation implements ProcessingService{
  private String metadata;


  public TokenAnnotation() {
      metadata = generateMetadata();
  }

  /**
   * Generate Metadata
   * Input format: TEXT/LAPPS
   * Output format: LAPPS
   * Annotation format: Uri.TOKEN
   * @return
   */
  private String generateMetadata() {
      // Create and populate the metadata object
      ServiceMetadata metadata = new ServiceMetadata();

      // Populate metadata using setX() methods
      metadata.setName(this.getClass().getName());
      metadata.setDescription("Token Annotation");
      metadata.setVersion("1.0.0-SNAPSHOT");
      metadata.setVendor("http://www.lappsgrid.org");
      metadata.setLicense(Uri.APACHE2);

      // JSON for input information
      IOSpecification requires = new IOSpecification();
      requires.addFormat(Uri.TEXT);           // Plain text (form)
      requires.addFormat(Uri.LAPPS);            // LIF (form)
      requires.addLanguage("en");             // Source language
      requires.setEncoding("UTF-8");

      // JSON for output information
      IOSpecification produces = new IOSpecification();
      produces.addFormat(Uri.LAPPS);          // LIF (form) synonymous to LIF
      produces.addAnnotation(Uri.TOKEN);      // Tokens (contents)
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
   * Tokenize the question and answers by whitespace
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
      
      //List<View> views = container.getViews();
      View view = container.getView(0);
      List<Annotation> annotations = view.getAnnotations();
      
      View newview = container.newView();
      
      int id = -1;
      for(int i = 0; i < annotations.size(); i++){
        Annotation temp = annotations.get(i);
        String type = temp.getFeature("Type");
        
        
          String ques = temp.getFeature(Uri.SENTENCE);
          String[] words = ques.trim().split("\\s+");
          
          int start = 0;
          for (String word : words) {
              start = ques.indexOf(word, start);  
            
              
              if (start < 0) {
                  return new Data<String>(Uri.ERROR, "Unable to match word: " + word).asJson();
              }
              
              int end = start + word.length();
              word = word.replaceAll("\\W", "");

              Annotation a = newview.newAnnotation(temp.getId()+"-tok" + (++id), Uri.TOKEN, start, end);
              a.addFeature(Features.Token.WORD, word);
              a.addFeature("Group", temp.getId());
             
              
          }
        }
              
     
      

      // Step #6: Update the view's metadata. Each view contains metadata about the
      // annotations it contains, in particular the name of the tool that produced the
      // annotations.
      newview.addContains(Uri.TOKEN, this.getClass().getName(), "tokenannotation");

      // Step #7: Create a DataContainer with the result.
      data = new DataContainer(container);

      // Step #8: Serialize the data object and return the JSON.
      return data.asPrettyJson();
  }
}
