package de.fhdw.ergoholics.brainphaser.logic;

import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.fhdw.ergoholics.brainphaser.BrainPhaserApplication;
import de.fhdw.ergoholics.brainphaser.BuildConfig;
import de.fhdw.ergoholics.brainphaser.database.ChallengeDataSource;
import de.fhdw.ergoholics.brainphaser.database.CompletionDataSource;
import de.fhdw.ergoholics.brainphaser.model.Category;
import de.fhdw.ergoholics.brainphaser.model.Challenge;
import de.fhdw.ergoholics.brainphaser.model.Completion;
import de.fhdw.ergoholics.brainphaser.model.Settings;
import de.fhdw.ergoholics.brainphaser.model.User;

/**
 * Created by Daniel Hoogen on 25/02/2016.
 *
 * This class contains the logic for reading due challenges from the databases
 */
public class DueChallengeLogic {
    private User mUser;

    public DueChallengeLogic(User user) {
        mUser = user;
    }

    /**
     * Returns the amount of due challenges for each category id.
     * @return array that maps category counts to categories
     */
    public LongSparseArray<Integer> getDueChallengeCounts(List<Category> categories) {
        LongSparseArray<Integer> dueChallengeCounts = new LongSparseArray<>();
        for (Category category : categories) {
            int challengesDueCount = getDueChallenges(category.getId()).size();
            dueChallengeCounts.put(category.getId(), challengesDueCount);
        }
        return dueChallengeCounts;
    }

    /**
     * Returns a list with the ids of all due challenges of the given user in the category with the
     * given id. If the given category id is -1, the due challenges of all categories will be
     * returned. Challenges without entries in the completed table will be returned and the missing
     * entries will be created.
     * @param categoryId the id of the category whose due challenges will be returned
     * @return List object containing the ids of all due challenges
     */
    public List<Long> getDueChallenges(long categoryId) {
        //Create list that will be returned in the end
        List<Long> dueChallenges = new ArrayList<>();

        //Get due challenges that have entries in the database
        addDueChallengesByCategory(dueChallenges, categoryId);

        //Create missing entries
        createMissingCompletedEntriesByCategory(dueChallenges, mUser, categoryId);

        //Return list
        return dueChallenges;
    }

    /**
     * Adds the ids of all due challenges of the given user in the category with the given id to
     * the given list. If the given category id is -1, the due challenges of all categories will be
     * added.
     * @param dueChallenges the list object the due challenges will be added to
     * @param categoryId the id of the category whose due challenges will be returned
     */
    private void addDueChallengesByCategory(List<Long> dueChallenges,
                                                   long categoryId) {
        //Create objects
        Date now = new Date();
        Date timebox;

        //Check for each stage if their challenges are due
        for (int stage = 1; stage<=6; stage++) {
            //Get the users completions in the stage
            List<Completion> completedInStage =
                    CompletionDataSource.getInstance().getByUserAndStage(mUser, stage);

            //Get the timebox for this stage
            timebox = getTimeboxByStage(mUser.getSettings(), stage);

            //Check for all challenges if they are due and in the correct category
            for (Completion completed : completedInStage) {
                Date lastCompleted = completed.getLastCompleted();

                if (now.getTime() - lastCompleted.getTime() >= timebox.getTime()) {
                    if (categoryId==-1 || categoryId ==
                            completed.getChallengeCompletions().getCategoryId()) {
                        dueChallenges.add(completed.getChallengeId());
                    }
                }
            }
        }
    }

    /**
     * Adds the ids of all due challenges without entries in the completed table to the given list.
     * The missing entries will also be created in the completed table.
     * @param dueChallenges the list object the due challenges will be added to
     * @param user the user whose due challenges will be added
     * @param categoryId the id of the category whose due challenges will be added
     */
    private void createMissingCompletedEntriesByCategory(List<Long> dueChallenges,
                                                                User user, long categoryId) {
        //Create objects
        Date now = new Date();

        //Get uncompleted challenges
        List<Challenge> notCompletedYet = ChallengeDataSource.getInstance().getUncompletedChallenges(user);

        //Calculate the lastCompleted time which needs to be set for making the challenge due
        Date dateChallengesDue = new Date(now.getTime() -
                getTimeboxByStage(user.getSettings(), 1).getTime());

        /*
        If categoryId is -1 or matches the challenge's id, the challenge will be added to the
        list and an entry will be created
        */
        for (Challenge challenge : notCompletedYet) {
            if (categoryId == -1 || challenge.getCategoryId() == categoryId) {
                Completion completed = new Completion(null, 1, dateChallengesDue, user.getId(),
                        challenge.getId());
                CompletionDataSource.getInstance().create(completed);
                dueChallenges.add(challenge.getId());
            }
        }
    }

    /**
     * Returns the timebox of the given stage in the given Settings object
     * @param settings Settings object whose timebox will be returned
     * @param stage number of the stage whose timebox will be returned
     * @return the Date object containing the timebox of the given settings object
     */
    @NonNull
    private Date getTimeboxByStage(Settings settings, int stage) {
        switch (stage) {
            case 1:
                return settings.getTimeBoxStage1();
            case 2:
                return settings.getTimeBoxStage2();
            case 3:
                return settings.getTimeBoxStage3();
            case 4:
                return settings.getTimeBoxStage4();
            case 5:
                return settings.getTimeBoxStage5();
            case 6:
                return settings.getTimeBoxStage6();
            default:
                throw new IllegalArgumentException("Attempting to get invalid timebox " + stage);
        }
    }
}