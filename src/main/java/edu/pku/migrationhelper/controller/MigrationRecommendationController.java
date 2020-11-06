package edu.pku.migrationhelper.controller;

import edu.pku.migrationhelper.data.LibraryMigrationCandidate;
import edu.pku.migrationhelper.data.lio.LioProject;
import edu.pku.migrationhelper.data.web.MigrationRecommendation;
import edu.pku.migrationhelper.data.web.VersionControlReference;
import edu.pku.migrationhelper.repository.LibraryMigrationCandidateRepository;
import edu.pku.migrationhelper.repository.LioProjectRepository;
import edu.pku.migrationhelper.service.EvaluationService;
import edu.pku.migrationhelper.service.GroupArtifactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class MigrationRecommendationController {
    private final LibraryMigrationCandidateRepository candidateRepository;
    private final LioProjectRepository lioProjectRepository;
    private final MigrationRecommendationAssembler assembler;
    private final GroupArtifactService groupArtifactService;
    private final EvaluationService evaluationService;

    public MigrationRecommendationController(
            @Autowired LibraryMigrationCandidateRepository candidateRepository,
            @Autowired LioProjectRepository lioProjectRepository,
            @Autowired MigrationRecommendationAssembler assembler,
            @Autowired GroupArtifactService groupArtifactService,
            @Autowired EvaluationService evaluationService
    ) {
        this.candidateRepository = candidateRepository;
        this.lioProjectRepository = lioProjectRepository;
        this.assembler = assembler;
        this.groupArtifactService = groupArtifactService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/recommend-one")
    public EntityModel<MigrationRecommendation> getRecommendation(
            @RequestParam(name="fromLib") String fromLib,
            @RequestParam(name="toLib") String toLib
    ) {
        if (!groupArtifactService.exist(fromLib)) {
            throw new ResourceNotFoundException("fromLib " + fromLib + " does not exist");
        }
        if (!groupArtifactService.exist(toLib)) {
            throw new ResourceNotFoundException("toLib " + toLib + " does not exist");
        }

        LibraryMigrationCandidate candidate = candidateRepository.findByFromIdAndToId(
              groupArtifactService.getIdByName(fromLib),
              groupArtifactService.getIdByName(toLib)
        ).orElseThrow(() -> new ResourceNotFoundException(
                String.format("Recommendation entry does not exist: fromId = %s, toLib = %s", fromLib, toLib)
        ));
        return assembler.toModel(fromLibraryMigrationCandidate(candidate));
    }

    @GetMapping("/recommend")
    public PagedModel<EntityModel<MigrationRecommendation>> getRecommendation(
            @RequestParam(name="fromLib") String fromLib,
            @RequestParam(name="page") int pageNum,
            @RequestParam(name="size") int pageSize
    ) {
        if (pageNum < 0) {
            throw new IllegalArgumentException("pageNum must be greater than zero");
        }
        if (!groupArtifactService.exist(fromLib)) {
            throw new ResourceNotFoundException("fromLib " + fromLib + " does not exist");
        }
        if (pageSize >= 1024) {
            throw new IllegalArgumentException("pageSize too large, must < 1024");
        }

        PageRequest request = PageRequest.of(pageNum, pageSize);
        Page<LibraryMigrationCandidate> recPage = candidateRepository.findByFromIdOrderByConfidenceDesc(
                groupArtifactService.getIdByName(fromLib),
                request
        );

        if (!recPage.hasContent()) {
            throw new ResourceNotFoundException(fromLib + " does not have any recommendation result at page "
                    + pageNum + " with page size " + pageSize);
        }

        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                recPage.getSize(),
                recPage.getNumber(),
                recPage.getTotalElements(),
                recPage.getTotalPages()
        );

        List<EntityModel<MigrationRecommendation>> recs = recPage.stream()
                .map(this::fromLibraryMigrationCandidate)
                .peek(x -> x.getRefs().clear()) // avoid too-large payload
                .map(assembler::toModel)
                .collect(Collectors.toList());

        Link self = linkTo(methodOn(MigrationRecommendationController.class).getRecommendation(
                fromLib, pageNum, pageSize)).withSelfRel();
        Link first = linkTo(methodOn(MigrationRecommendationController.class).getRecommendation(
                fromLib, 0, pageSize)).withRel("first");
        Link next = linkTo(methodOn(MigrationRecommendationController.class).getRecommendation(
                fromLib, (pageNum + 1) % recPage.getTotalPages(), pageSize)).withRel("next");
        Link last = linkTo(methodOn(MigrationRecommendationController.class).getRecommendation(
                fromLib, recPage.getTotalPages() - 1, pageSize)).withRel("last");

        return new PagedModel<>(recs, metadata, self, first, next, last);
    }

    @GetMapping("/library")
    public LioProject getLibraryInfo(@RequestParam(name="lib") String lib) {
        return lioProjectRepository.findByName(lib).orElseThrow(
                () -> new IllegalArgumentException(lib + " does not exist in our db")
        );
    }

    @GetMapping("/libraries-with-prefix")
    public List<String> getLibrariesWithPrefix(@RequestParam(name="prefix") String prefix) {
        return groupArtifactService.getNamesWithPrefix(prefix, 20);
    }

    @GetMapping("/libraries-similar")
    public List<String> getLibrariesSimilar(@RequestParam(name="lib") String lib) {
        return groupArtifactService.getMostSimilarNames(lib, 20);
    }

    private MigrationRecommendation fromLibraryMigrationCandidate(LibraryMigrationCandidate candidate) {
        List<VersionControlReference> refs = candidate.possibleCommitList.stream()
                .map(x -> new VersionControlReference(
                        evaluationService.isConfirmedMigration(x[0], x[1], x[2], x[3]),
                        true,
                        x[0], x[1], x[2], x[3]))
                .sorted((a, b) -> Integer.compare(
                        (b.isConfirmed() ? 1 : 0) * 10 +  (b.isPossible() ? 1 : 0),
                        (a.isConfirmed() ? 1 : 0) * 10 +  (a.isPossible() ? 1 : 0)
                )).collect(Collectors.toList());

        return new MigrationRecommendation(
                groupArtifactService.getGroupArtifactById(candidate.fromId).getGroupArtifactId(),
                groupArtifactService.getGroupArtifactById(candidate.toId).getGroupArtifactId(),
                candidate.confidence,
                candidate.ruleCountSameCommit,
                candidate.possibleCommitList.size(),
                candidate.methodChangeCount,
                candidate.ruleSupportByMaxSameCommit,
                candidate.commitMessageSupport,
                candidate.commitDistanceSupport,
                candidate.methodChangeSupportByMax,
                refs
        );
    }

    @Component
    public static class MigrationRecommendationAssembler
            implements RepresentationModelAssembler<MigrationRecommendation, EntityModel<MigrationRecommendation>>
    {
        @Override
        public EntityModel<MigrationRecommendation> toModel(MigrationRecommendation rec) {
            return new EntityModel<>(
                    rec,
                    linkTo(methodOn(MigrationRecommendationController.class).getRecommendation(
                            rec.getFromLib(), rec.getToLib())).withSelfRel()
            );
        }
    }
}
