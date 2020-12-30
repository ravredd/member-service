package com.sams.merch.engenable.memberservice.web;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import com.sams.merch.engenable.memberservice.domain.Member;
import com.sams.merch.engenable.memberservice.service.MemberService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/members")
@Tag(name = "member", description = "`Members` endpoints for Member information")
public class MemberController {
    
    private MemberService memberService;

    private static final String MEMBERS = "/members";// NOSONAR
    private static final String ID = "/{id}";// NOSONAR
    private static final String MEMBER = MEMBERS + ID;

    public MemberController(MemberService memberService){
        this.memberService = memberService;
    }

    /**
     * Get all members
     *
     * @return Members list
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all members and their information.",
    description = "Gets all members.", 
    responses = {
        @ApiResponse(responseCode="200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Member.class)), 
        examples = { @ExampleObject( name = "Full return of members.", summary = "All Members", value = ApiStaticExamples.MEMBER_ARRAY_EXAMPLE)}))
    })
    public ResponseEntity<List<Member>> getAllMembers(UriComponentsBuilder uriComponentsBuilder) {
        return ResponseEntity.ok().location(createMembersLocationUri(uriComponentsBuilder)).body(memberService.findAll());
    }
    
    /**
     * Get member by upcNbr
     *
     * @param upcNbr
     * @return Member
     */
    @GetMapping(value = ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a member information", 
    description = "Returns a given member information given upcNbr for member.", 
    responses = {
        @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Member.class), 
                    examples = { @ExampleObject( name = "Member Example 1", value = ApiStaticExamples.MEMBER_EXAMPLE1),
                                    @ExampleObject( name = "Member Example 2", value = ApiStaticExamples.MEMBER_EXAMPLE2),
                                    @ExampleObject( name = "Member Example 3", value = ApiStaticExamples.MEMBER_EXAMPLE3)})),
        @ApiResponse(responseCode = "404", description = "Member with such upcNbr doesn't exists.", content = @Content( mediaType = MediaType.TEXT_PLAIN_VALUE)),
        @ApiResponse(responseCode = "400", description = "Invalid input. upcNbr required.", content=@Content(mediaType=MediaType.TEXT_PLAIN_VALUE))})
    public ResponseEntity<Member> getMember(
            @Parameter(name = "id", allowEmptyValue = false, description = "Member id", required = true) 
            @PathVariable(name = "id", required = true) Long id, UriComponentsBuilder uriComponentsBuilder) {
        return memberService.findById(id).map(
                member -> ResponseEntity.ok().location(createMemberLocationUri(uriComponentsBuilder, member)).body(member))
                .orElse(ResponseEntity.notFound().build());
    }

    
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Save a new member or update an existing.",
    description = "Save a new member if doesnt exist or updates existing.",
    responses = {
        @ApiResponse(responseCode = "201", content=@Content(schema =@Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input for member. firstName, lastName and email required.", content=@Content(mediaType=MediaType.TEXT_PLAIN_VALUE))
    })
    public ResponseEntity<Member> createMember(@RequestBody @Valid Member member, UriComponentsBuilder uriComponentsBuilder){
        Member newMember = memberService.save(member);
        return ResponseEntity.created(createMemberLocationUri(uriComponentsBuilder, member)).body(newMember);
    }
    
    @DeleteMapping(value = ID, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Deletes an member for given id.",
    description = "Deletes anmember for a given id.",
    responses = {
        @ApiResponse(responseCode = "200", description = "Member was deleted.", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE))
    })
    public ResponseEntity<String> deleteMember(@PathVariable Long id){
        memberService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private URI createMembersLocationUri(UriComponentsBuilder uriComponentsBuilder) {
        return uriComponentsBuilder.path(MEMBERS).build().toUri();
    }

    private URI createMemberLocationUri(UriComponentsBuilder uriComponentsBuilder, Member member) {
        return uriComponentsBuilder.path(MEMBER).buildAndExpand(member.getId()).toUri();
    }
}
