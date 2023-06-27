package com.mobichord.ftps.message;

import com.appchord.messages.Event;
import com.mobichord.ftps.data.DownloadSource;
import com.mobichord.processing.common.data.SiteBase;
import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class CreateAttachmentsEvent extends Event {

    /**
     * Might be null in case of S3 source
     */
    private SiteBase sourceSite;

    @NotNull
    private DownloadSource source;

    @NotNull
    private SiteBase targetSite;

    @NotEmpty
    private String targetTable;

    @NotNull
    private List<String> attachments;

    private boolean cleanBeforeCreate;
}
