package edu.pku.migrationhelper.assembler;

import edu.pku.migrationhelper.controller.MigrationRecommendationController;
import edu.pku.migrationhelper.data.web.MigrationRecommendation;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class MigrationRecommendationAssembler
        implements RepresentationModelAssembler<MigrationRecommendation, EntityModel<MigrationRecommendation>>
{
    @Override
    public EntityModel<MigrationRecommendation> toModel(MigrationRecommendation rec) {
        return new EntityModel<>(
                rec,
                linkTo(methodOn(MigrationRecommendationController.class).getOne(rec.getFromLib(), rec.getToLib())).withSelfRel()
        );
    }
}
