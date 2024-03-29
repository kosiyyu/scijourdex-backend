package com.project.app.api.v1.service;

import com.project.app.api.v1.dto.CustomSearchDto;
import com.project.app.api.v1.dto.SearchTokenDto;
import com.project.app.api.v1.entity.Journal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class QueryService {
    private final EntityManager entityManager;

    public QueryService(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    private String orderByCondition(String orderByArgument, boolean isDesc){
        String condition;
        switch(orderByArgument) {
            case "Title 1" -> condition = "order by title1 " + (isDesc ? "desc " : "asc ");
            case "Issn 1" -> condition = "order by issn1 " + (isDesc ? "desc " : "asc ");
            case "E-issn 1" -> condition = "order by eissn1 " + (isDesc ? "desc " : "asc ");
            case "Title 2" -> condition = "order by title2 " + (isDesc ? "desc " : "asc ");
            case "Issn 2" -> condition = "order by issn2 " + (isDesc ? "desc " : "asc ");
            case "E-issn 2" -> condition = "order by eissn2 " + (isDesc ? "desc " : "asc ");
            case "Points" -> condition = "order by points " + (isDesc ? "desc " : "asc ");
            case "CiteScore" -> condition = "order by cite_score " + (isDesc ? "desc " : "asc ");
            case "Impact factor" -> condition = "order by impact_factor " + (isDesc ? "desc " : "asc ");
            default -> condition = "order by id " + (isDesc ? "desc " : "asc ");
        }
        return condition;
    }

    private String whereCondition(List<String> searchStrings, List<String> tagStrings, boolean isOr, String similarityString, double similarityValue){
        StringBuilder stringBuilder = new StringBuilder();

        String searchStringsOperator = isOr ? "OR" : "AND";

        boolean isWhereClauseNeeded = true;

        if (!searchStrings.isEmpty()) {
            stringBuilder.append("WHERE (");
            for (int i = 0; i < searchStrings.size(); i++) {
                if (i > 0) {
                    stringBuilder
                            .append(" ")
                            .append(searchStringsOperator)
                            .append(" ");
                }
                stringBuilder
                        .append("(j.title1 ILIKE '%")
                        .append(searchStrings.get(i))
                        .append("%' ")
                        .append("OR j.title2 ILIKE '%")
                        .append(searchStrings.get(i))
                        .append("%')");
            }
            stringBuilder.append(")");
            isWhereClauseNeeded = false;
        }

        if (!tagStrings.isEmpty()) {
            if(isWhereClauseNeeded){
                stringBuilder.append("WHERE (");
            }
            else {
                stringBuilder.append(" AND (");
            }
            for (int i = 0; i < tagStrings.size(); i++) {
                if (i > 0) {
                    stringBuilder.append(" OR ");
                }
                stringBuilder
                        .append("t.value = '")
                        .append(tagStrings.get(i))
                        .append("'");
            }
            stringBuilder
                    .append(") ")
                    .append("GROUP BY j.id ")
                    .append("HAVING COUNT(DISTINCT t.value) = ")
                    .append(tagStrings.size());
        }

        if (!(similarityString.isEmpty() || similarityValue > 1 || similarityValue < 0)) {
            if(isWhereClauseNeeded){
                stringBuilder.append("WHERE (");
            }
            else {
                stringBuilder.append(" AND (");
            }
            stringBuilder
                    .append("SIMILARITY(LOWER(COALESCE(j.aims_and_scope, '')), LOWER('")
                    .append(similarityString)
                    .append("')) >= ")
                    .append(similarityValue)
                    .append(")");
        }
        return stringBuilder.toString();
    }

    private long getCount(String whereCondition){
        String countQuery =
                "SELECT COUNT(DISTINCT Sub.id) " +
                "FROM (" +
                "SELECT j.id, j.aims_and_scope " +
                "FROM journal j " +
                "LEFT JOIN journal_tag t_g ON j.id = t_g.journal_id " +
                "LEFT JOIN tag t ON t_g.tag_id = t.id " +
                "LEFT JOIN metadata m ON j.metadata_id = m.id " +
                whereCondition +
                ") AS Sub";
        Query query = entityManager.createNativeQuery(countQuery, Long.class);
        return (long) query.getSingleResult();
    }

    private List<Journal> getJournals(String whereCondition, String orderByCondition, int limit, int offset){
        String mainQuery =
                "SELECT DISTINCT Sub.id, Sub.title1, Sub.issn1, Sub.eissn1, Sub.title2, Sub.issn2, Sub.eissn2, Sub.points, Sub.metadata_id, Sub.aims_and_scope, Sub.cite_score, Sub.impact_factor " +
                        "FROM (" +
                        "SELECT j.id, j.title1, j.issn1, j.eissn1, j.title2, j.issn2, j.eissn2, j.points, j.metadata_id, j.aims_and_scope, j.cite_score, j.impact_factor " +
                        "FROM journal j " +
                        "LEFT JOIN journal_tag t_g ON j.id = t_g.journal_id " +
                        "LEFT JOIN tag t ON t_g.tag_id = t.id " +
                        "LEFT JOIN metadata m ON j.metadata_id = m.id " +
                        whereCondition +
                        ") AS Sub " +
                        orderByCondition +
                        "LIMIT " + limit + " " +
                        "OFFSET " + offset;

        return entityManager.createNativeQuery(mainQuery, Journal.class).getResultList();
    }

    public CustomSearchDto query(SearchTokenDto searchTokenDto) {
        int limit = Math.min(searchTokenDto.pageSize(), 100);
        int offset = searchTokenDto.pageIndex();
        if (offset < 0) {
            return new CustomSearchDto(0, offset, Collections.emptyList());
        }

        String whereCondition = whereCondition(searchTokenDto.searchStrings(), searchTokenDto.tagStrings(), searchTokenDto.isOr(), searchTokenDto.similarityString(), searchTokenDto.similarityValue());
        String orderByCondition = orderByCondition(searchTokenDto.orderByArgument(), searchTokenDto.isDescSort());

        long count = getCount(whereCondition);
        long numberOfPages = (long) Math.ceil((double) count / limit);
        if(offset > numberOfPages || offset < 0) {
            return new CustomSearchDto(0, 0, Collections.emptyList());
        }

        offset *= limit;

        List<Journal> journals = getJournals(whereCondition, orderByCondition, limit, offset);
        return new CustomSearchDto(numberOfPages,searchTokenDto.pageIndex(),journals);
    }
}
