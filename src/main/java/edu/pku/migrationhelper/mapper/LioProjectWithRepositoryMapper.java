package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.LioProjectWithRepository;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by xuyul on 2020/2/16.
 */
@Mapper
public interface LioProjectWithRepositoryMapper {

    String tableName = "lio_project_with_repository";

    @Insert("<script>" +
            "insert  ignore into " + tableName + " " +
            "(id, platform, language, name, repository_url, repository_id, source_rank, " +
            "repository_star_count, repository_fork_count, repository_watchers_count, " +
            "repository_source_rank, dependent_projects_count, dependent_repositories_count) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.id}, #{e.platform}, #{e.language}, #{e.name}, #{e.repositoryUrl}, #{e.repositoryId}, #{e.sourceRank}, " +
            "#{e.repositoryStarCount}, #{e.repositoryForkCount}, #{e.repositoryWatchersCount}, " +
            "#{e.repositorySourceRank}, #{e.dependentProjectsCount}, #{e.dependentRepositoriesCount}) " +
            "</foreach> " +
            "</script>")
    int insert(List<LioProjectWithRepository> entities);

    @Select("<script>" +
            "select * from " + tableName + " where id = #{id}" +
            "</script>")
    LioProjectWithRepository findById(@Param("id") long id);

    @Select("<script>" +
            "select id from " + tableName + " " +
            "order by source_rank desc " +
            "limit #{limit}" +
            "</script>")
    List<Long> selectIdOrderBySourceRankLimit(@Param("limit") int limit);

    @Select("<script>" +
            "select id from " + tableName + " " +
            "order by repository_star_count desc " +
            "limit #{limit}" +
            "</script>")
    List<Long> selectIdOrderByRepositoryStarCountLimit(@Param("limit") int limit);

    @Select("<script>" +
            "select id from " + tableName + " " +
            "order by repository_fork_count desc " +
            "limit #{limit}" +
            "</script>")
    List<Long> selectIdOrderByRepositoryForkCountLimit(@Param("limit") int limit);

    @Select("<script>" +
            "select id from " + tableName + " " +
            "order by repository_watchers_count desc " +
            "limit #{limit}" +
            "</script>")
    List<Long> selectIdOrderByRepositoryWatchersCountLimit(@Param("limit") int limit);

    @Select("<script>" +
            "select id from " + tableName + " " +
            "order by repository_source_rank desc " +
            "limit #{limit}" +
            "</script>")
    List<Long> selectIdOrderByRepositorySourceRankLimit(@Param("limit") int limit);

    @Select("<script>" +
            "select id from " + tableName + " " +
            "order by dependent_projects_count desc " +
            "limit #{limit}" +
            "</script>")
    List<Long> selectIdOrderByDependentProjectsCountLimit(@Param("limit") int limit);

    @Select("<script>" +
            "select id from " + tableName + " " +
            "order by dependent_repositories_count desc " +
            "limit #{limit}" +
            "</script>")
    List<Long> selectIdOrderByDependentRepositoriesCountLimit(@Param("limit") int limit);
}
