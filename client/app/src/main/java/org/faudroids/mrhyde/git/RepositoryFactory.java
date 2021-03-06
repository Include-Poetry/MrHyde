package org.faudroids.mrhyde.git;

import android.content.Context;
import android.os.Environment;

import com.google.common.base.Optional;

import org.eclipse.egit.github.core.User;
import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.bitbucket.BitbucketLink;
import org.faudroids.mrhyde.bitbucket.BitbucketRepository;
import org.faudroids.mrhyde.bitbucket.BitbucketUser;
import org.faudroids.mrhyde.gitlab.GitLabProject;
import org.faudroids.mrhyde.gitlab.GitLabProjectOwner;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Factory pattern for creating {@link Repository} instances.
 */
@Singleton
public class RepositoryFactory {

  private final Context context;

  @Inject
  RepositoryFactory(Context context) {
    this.context = context;
  }

  public Repository fromGitHubRepository(org.eclipse.egit.github.core.Repository gitHubRepo) {
    String name = gitHubRepo.getName();
    RepositoryOwner owner = fromGitHubUser(gitHubRepo.getOwner());
    File rootDir = getRootDir(GitHostingProvider.GITHUB, owner, name);
    return new Repository(
        name,
        gitHubRepo.getCloneUrl(),
        false,
        AuthType.GITHUB_OAUTH2_ACCESS_TOKEN,
        GitHostingProvider.GITHUB,
        rootDir,
        Optional.of(owner)
    );
  }

  public RepositoryOwner fromGitHubUser(User gitHubUser) {
    return new RepositoryOwner(
        gitHubUser.getLogin(),
        Optional.fromNullable(gitHubUser.getAvatarUrl())
    );
  }

  public Repository fromBitbucketRepository(BitbucketRepository bitbucketRepo) {
    String name = bitbucketRepo.getName();
    RepositoryOwner owner = fromBitbucketUser(bitbucketRepo.getOwner());
    File rootDir = getRootDir(GitHostingProvider.BITBUCKET, owner, name);
    String cloneUrl = null;
    for (BitbucketLink link : bitbucketRepo.getLinks().getClone()) {
      if (link.getName().equals("https")) {
        cloneUrl = link.getHref();
      }
    }
    if (cloneUrl == null) throw new IllegalStateException("No clone url for " + bitbucketRepo.getName());

    return new Repository(
        name,
        cloneUrl,
        false,
        AuthType.BITBUCKET_OAUTH2_ACCESS_TOKEN,
        GitHostingProvider.BITBUCKET,
        rootDir,
        Optional.of(owner)
    );
  }

  public RepositoryOwner fromBitbucketUser(BitbucketUser bitbucketUser) {
    String avatarUrl = bitbucketUser.getLinks().getAvatar() == null
        ? null
        : bitbucketUser.getLinks().getAvatar().getHref();
    bitbucketUser.getLinks().getAvatar();
    return new RepositoryOwner(
        bitbucketUser.getUsername(),
        Optional.fromNullable(avatarUrl)
    );
  }

  public Repository fromGitLabProject(GitLabProject gitLabProject) {
    String name = gitLabProject.getName();
    RepositoryOwner owner = fromGitLabUser(gitLabProject.getOwner());
    File rootDir = getRootDir(GitHostingProvider.GITLAB, owner, name);
    return new Repository(
        name,
        gitLabProject.getHttpUrlToRepo(),
        false,
        AuthType.GITLAB_OAUTH2_ACCESS_TOKEN,
        GitHostingProvider.GITLAB,
        rootDir,
        Optional.of(owner)
    );
  }

  public RepositoryOwner fromGitLabUser(GitLabProjectOwner gitLabUser) {
    return new RepositoryOwner(gitLabUser.getName(), Optional.absent());
  }

  private File getRootDir(GitHostingProvider hostingProvider, RepositoryOwner owner, String repoName) {
    return new File(
        new File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name)),
        String.format("%s/%s/%s", hostingProvider.getRootDirName(), owner.getUsername(), repoName)
    );
  }
}
