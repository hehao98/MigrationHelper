package anonymous.migrationhelper.mapper;

import anonymous.migrationhelper.data.LioProjectWithRepository;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Created by xxx on 2020/2/16.
 */
@Mapper
public interface LioProjectWithRepositoryMapper {

    String tableName = "lio_project_with_repository";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                                             `id` bigint(20) NOT NULL,\n" +
            "                                             `platform` varchar(255) DEFAULT NULL,\n" +
            "                                             `language` varchar(255) DEFAULT NULL,\n" +
            "                                             `name` varchar(255) DEFAULT NULL,\n" +
            "                                             `repository_url` varchar(255) DEFAULT NULL,\n" +
            "                                             `repository_id` bigint(20) DEFAULT NULL,\n" +
            "                                             `source_rank` int(11) DEFAULT NULL,\n" +
            "                                             `repository_star_count` int(11) DEFAULT NULL,\n" +
            "                                             `repository_fork_count` int(11) DEFAULT NULL,\n" +
            "                                             `repository_watchers_count` int(11) DEFAULT NULL,\n" +
            "                                             `repository_source_rank` int(11) DEFAULT NULL,\n" +
            "                                             `dependent_projects_count` int(11) DEFAULT NULL,\n" +
            "                                             `dependent_repositories_count` int(11) DEFAULT NULL,\n" +
            "                                             PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" +
            "</script>")
    void createTable();

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

    @Select("<script>" +
            "select * from " + tableName + " order by id limit #{offset}, #{limit} " +
            "</script>")
    List<LioProjectWithRepository> findList(@Param("offset") long offset, @Param("limit") int limit);
}
