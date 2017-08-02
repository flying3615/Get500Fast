package fast500;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetFast500 {

    public static void main(String args[]) throws IOException {

        //Loading an existing document
        GetFast500 getFast500 = new GetFast500();

        CompletableFuture<List<Company>> future_2014
                = CompletableFuture.supplyAsync(()->getFast500.analysis("2014_fast500.pdf", "New Zealand", "Software","2014"))
                .handle((result,err)->result!=null?result:Collections.emptyList());

        CompletableFuture<List<Company>> future_2015
                = CompletableFuture.supplyAsync(()->getFast500.analysis("2015_fast500.pdf", "New Zealand", "Software","2015"))
                .handle((result,err)->result!=null?result:Collections.emptyList());

        CompletableFuture<List<Company>> future_2016
                = CompletableFuture.supplyAsync(()->getFast500.analysis("2016_fast500.pdf", "New Zealand", "Software","2016"))
                    .handle((result,err)->result!=null?result:Collections.emptyList());


        Set<Company> companies = Stream.of(future_2014, future_2015, future_2016)
                .map(CompletableFuture::join)
                .flatMap(l->l.stream()) //flatten in java
                .sorted(Comparator.comparingInt(c -> -Integer.parseInt(c.growth))) //reverse order
                .collect(Collectors.toSet());

        System.out.println("Total companies "+companies.size());

        companies.forEach(c->{
                    if(c.getInfoEmailAddress().isPresent()){
                        System.out.println("++++Get email from "+c+ "Email: "+c.getInfoEmailAddress().get());
                    }else{
//                        System.out.println("----Email for "+c.name+" "+c.website+ " not found");
                    }
                });


    }

    private List<Company> analysis(String fileName, String country, String field, String year) {
        List<String> lineList;
        List<Company> result;
        try {
            lineList = Arrays.asList(this.getFileContent(fileName).split("\n"));
            String patternString = "([0-9]+) (.*) ([0-9]+)% (www.*)";

            Pattern pattern = Pattern.compile(patternString);
            result = lineList.stream()
                    .filter(line -> pattern.matcher(line).matches() && line.contains(country) && line.contains(field))
                    .map(line -> {
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        String rank = matcher.group(1)+" in "+year;
                        String name = matcher.group(2).replace(field,"").replace(country,"").trim(); //delete field&country from name
                        String growth = matcher.group(3);
                        String website = matcher.group(4);
                        return new Company(rank, name, website, country, year, growth,field);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return result;
    }


    private String getFileContent(String fileName) throws IOException {

        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());

        PDDocument document = PDDocument.load(file);

        //Instantiate PDFTextStripper class
        PDFTextStripper pdfStripper = new PDFTextStripper();

        //Retrieving text from PDF document
        String text = pdfStripper.getText(document);

        //Closing the document
        document.close();

        return text;

    }
}
