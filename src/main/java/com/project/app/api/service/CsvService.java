package com.project.app.api.service;

import com.project.app.api.entity.Article;
import com.project.app.api.entity.Tag;
import org.apache.el.stream.Stream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.IntStream;

@Service
public class CsvService {
    final private TagService tagService;
    final private ArticleService articleService;

    public CsvService(TagService tagService, ArticleService articleService){
        this.tagService = tagService;
        this.articleService = articleService;
    }
    public void loadCsv(byte[] bytes) throws IOException {

        final BufferedReader bufferedReader = new BufferedReader(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        int index = 0; // csv iteration
        String line;
        List<Tag> tags = null;
        while ((line = bufferedReader.readLine()) != null) {
            String[] arr = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
            if(index == 0){
                tags = Arrays.stream(Arrays.copyOfRange(arr, 9, arr.length))
                        .map(Tag::new)
                        .toList();
                tags = tagService.saveAll(tags);
            }
            else if(index == 1){
                index++;
                continue;
            }
            else {
                List<String> values = Arrays.stream(Arrays.copyOfRange(arr, 9, arr.length))
                        .toList();
                List<Tag> localTags = new ArrayList<>();
                for(int i = 0; i < values.size(); i++){
                    if(values.get(i).equals("x")){
                        localTags.add(tags.get(i));
                    }
                }
                Article article = new Article(arr[2], arr[3], arr[4], arr[5], arr[6], arr[7], Integer.parseInt(arr[8]), localTags);
                articleService.saveArticleWithUniqueTags(article);
            }
            index++;
        }
    }

    private static void extractMapValues(String line, int numberOfWords, Map<Integer, String> map) {
        int end = line.length();
        int wordCounter = 0;
        boolean isOpen = false;
        for (int i = line.length() - 1; i >= 0; i--) {
            if (line.charAt(i) == '"' && i == line.length() - 1) {
                end = i;
            } else if (line.charAt(i) == '"' && line.charAt(i - 1) == ',' && line.charAt(i - 2) == '"') {
                map.put(wordCounter, line.substring(i + 1, end));
                wordCounter++;
                end = i - 2;
                isOpen = true;
                i -= 2;
            } else if (line.charAt(i) == ',' && line.charAt(i - 1) == '"') {
                map.put(wordCounter, line.substring(i + 1, end));
                isOpen = true;
                wordCounter++;
                end = i - 1;
            } else if (line.charAt(i) == ',' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                map.put(wordCounter, line.substring(i + 2, end));
                isOpen = false;
                wordCounter++;
                end = i;
            } else if (line.charAt(i) == ',' && line.charAt(i - 1) != '"' && i + 1 < line.length() && line.charAt(i + 1) != '"' && !isOpen) {
                map.put(wordCounter, line.substring(i + 1, end));
                wordCounter++;
                end = i;
            } else if (i == 0) {
                map.put(wordCounter, line.substring(0, end));
                wordCounter++;
            }
            if (wordCounter == numberOfWords) {
                break;
            }
        }
    }

    private void extractArticleData(String line, int numberOfWords, int numberOfArgs, Map<Integer, String> map) {
        List<Tag> tags = new ArrayList<>();
        Article article = new Article();
        int end = line.length();
        int wordCounter = 0;
        boolean isOpen = false;
        boolean isXFound = false;
        boolean isArgChanged = false;
        String str = "";
        for (int i = line.length() - 1; i >= 0; i--) {
            if(line.charAt(i) == 'x' && !isXFound){
                isXFound = true;
            }
            if(wordCounter < numberOfWords - 1){
                if (line.charAt(i) == '"' && i == line.length() - 1) {
                    end = i;
                } else if (line.charAt(i) == '"' && line.charAt(i - 1) == ',' && line.charAt(i - 2) == '"') {
                    if(isXFound){
                        tags.add(new Tag(map.get(wordCounter)));
                    }
                    isXFound = false;
                    wordCounter++;
                    end = i - 2;
                    isOpen = true;
                    i -= 2;
                } else if (line.charAt(i) == ',' && line.charAt(i - 1) == '"') {
                    if(isXFound){
                        tags.add(new Tag(map.get(wordCounter)));
                    }
                    isXFound = false;
                    isOpen = true;
                    wordCounter++;
                    end = i - 1;
                } else if (line.charAt(i) == ',' && i + 1 < line.length() && line.charAt(i + 1) == '"') {///////////
                    if(isXFound){
                        tags.add(new Tag(map.get(wordCounter)));
                    }
                    isXFound = false;
                    isOpen = false;
                    wordCounter++;
                    end = i;
                } else if (line.charAt(i) == ',' && line.charAt(i - 1) != '"' && i + 1 < line.length() && line.charAt(i + 1) != '"' && !isOpen) {
                    if(isXFound){
                        tags.add(new Tag(map.get(wordCounter)));
                    }
                    isXFound = false;
                    wordCounter++;
                    end = i;
                } else if (i == 0) {
                    if(isXFound){
                        tags.add(new Tag(map.get(wordCounter)));
                    }
                    isXFound = false;
                    wordCounter++;
                }
            }
            else {
                if (line.charAt(i) == '"' && i == line.length() - 1) {
                    end = i;
                } else if (line.charAt(i) == '"' && line.charAt(i - 1) == ',' && line.charAt(i - 2) == '"') {
                    str = line.substring(i + 1, end);
                    isArgChanged = true;
                    wordCounter++;
                    end = i - 2;
                    isOpen = true;
                    i -= 2;
                } else if (line.charAt(i) == ',' && line.charAt(i - 1) == '"') {
                    str = line.substring(i + 1, end);
                    isArgChanged = true;
                    isOpen = true;
                    wordCounter++;
                    end = i - 1;
                } else if (line.charAt(i) == ',' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    str = line.substring(i + 2, end);
                    isArgChanged = true;
                    isOpen = false;
                    wordCounter++;
                    end = i;
                } else if (line.charAt(i) == ',' && line.charAt(i - 1) != '"' && i + 1 < line.length() && line.charAt(i + 1) != '"' && !isOpen) {
                    str = line.substring(i + 1, end);
                    isArgChanged = true;
                    wordCounter++;
                    end = i;
                } else if (i == 0) {
                    str = line.substring(0, end);
                    isArgChanged = true;
                    wordCounter++;
                }

                if(isArgChanged){
                    switch (wordCounter){
                        case 44 -> article.setPoints(Integer.parseInt(str));
                        case 45 -> article.setEissn2(str);
                        case 46 -> article.setIssn2(str);
                        case 47 -> article.setTitle2(str);
                        case 48 -> article.setEissn1(str);
                        case 49 -> article.setIssn1(str);
                        case 50 -> article.setTitle1(str);
                    }
                    isArgChanged = false;
                }

                if(!tags.isEmpty()){
                    article.setTags(tags);
                }
            }

            if (wordCounter == numberOfWords + numberOfArgs - 1) {
                break;
            }
        }
        try{
            articleService.saveArticleWithUniqueTags(article);
        }catch (Exception e){
            throw new RuntimeException("err_inside");
        }
    }

    public void extractCsvData(byte[] bytes) throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        int lineIndex = 0;
        String line;
        int numberOfWords = 44;
        int numberOfArgs = 7;
        Map<Integer, String> map = new HashMap<>();
        while ((line = bufferedReader.readLine()) != null) {
            // todo loops refactor: still
            if (lineIndex == 0) {
                extractMapValues(line, numberOfWords, map);
            }
            else if(lineIndex == 1) {
                lineIndex++;
                continue;
            }
            else {
                 extractArticleData(line, numberOfWords, numberOfArgs, map);
            }
            lineIndex++;
        }
    }
}
