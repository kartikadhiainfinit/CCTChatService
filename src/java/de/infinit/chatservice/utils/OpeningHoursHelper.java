package de.infinit.chatservice.utils;

import com.genesyslab.platform.applicationblocks.com.objects.CfgStatDay;
import com.genesyslab.platform.configuration.protocol.types.CfgFlag;
import com.genesyslab.platform.configuration.protocol.types.CfgObjectState;
import de.infinit.chatservice.communication.beans.ChatCategory;
import de.infinit.chatservice.configuration.beans.CategoryConfig;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sebesjir
 */
public class OpeningHoursHelper {

    public static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(OpeningHoursHelper.class);

    public static LinkedList<ChatCategory> evaluateCategoryAvailability(Map<String, CategoryConfig> configuredCategories) {
        LinkedList<ChatCategory> result = new LinkedList();

        Collection<CategoryConfig> values = configuredCategories.values();

        boolean isCategoryOpen;
        Calendar currentCalendar = Calendar.getInstance();
        
        for (CategoryConfig configuredCategory : values) {
            isCategoryOpen = isCategoryOpen(configuredCategory, currentCalendar);
            result.add(new ChatCategory(configuredCategory.getId(), configuredCategory.getDisplayName(), isCategoryOpen));
        }

        return result;
    }
    
    public static ChatCategory evaluateCategoryAvailability(CategoryConfig configuredCategory) {
        ChatCategory result;

        boolean isCategoryOpen;
        Calendar currentCalendar = Calendar.getInstance();
        
        isCategoryOpen = isCategoryOpen(configuredCategory, currentCalendar);
        result = new ChatCategory(configuredCategory.getId(), configuredCategory.getDisplayName(), isCategoryOpen);

        return result;
    }
    
    public static ChatCategory evaluateCategoryAvailability(Map<String, CategoryConfig> configuredCategories, String categoryId){
        CategoryConfig requiredCategory = null;
            for (Map.Entry<String, CategoryConfig> entry : configuredCategories.entrySet()) {
                CategoryConfig cat = entry.getValue();
                if (cat.getId().equals(categoryId)) {
                    requiredCategory = cat;
                }
            }
            if(requiredCategory == null){
                return null;
            }
            ChatCategory category = OpeningHoursHelper.evaluateCategoryAvailability(requiredCategory);
            return category;
    }

    public static Date getNextCategoryChangeTime(Map<String, CategoryConfig> configuredCategories) {
        long nextEvaluation = -1;

        // get current date
        Calendar currentCalendar = Calendar.getInstance();
        long curentDayMilis = currentCalendar.getTimeInMillis();
        int currentYear = currentCalendar.get(Calendar.YEAR);

        Collection<CategoryConfig> values = configuredCategories.values();

        for (CategoryConfig cofiguredCategory : values) {

            List<CfgStatDay> openingHours = cofiguredCategory.getOpeningHours();

            //evaluate next change time
            for (CfgStatDay statDay : openingHours) {
                if (statDay.getState() == CfgObjectState.CFGEnabled) {
                    Integer startTime = statDay.getStartTime();
                    long startTimeMilis = startTime * 60000L;
                    Integer endTime = statDay.getEndTime();
                    long endTimeMilis = endTime * 60000L;
                    
                    Calendar calendar = statDay.getDate();
                    if (statDay.getIsDayOfWeek() != CfgFlag.CFGTrue) {
                        if (calendar.get(Calendar.YEAR) > 1970) {
                            if (nextEvaluation < 0) {
                                if ((calendar.getTimeInMillis() + startTimeMilis) > curentDayMilis) {
                                    nextEvaluation = calendar.getTimeInMillis() + startTimeMilis;
                                } else if ((calendar.getTimeInMillis() + endTimeMilis) > curentDayMilis) {
                                    nextEvaluation = calendar.getTimeInMillis() + endTimeMilis;
                                }
                            } else {
                                if ((calendar.getTimeInMillis() + startTimeMilis) > curentDayMilis && (calendar.getTimeInMillis() + startTimeMilis) < nextEvaluation) {
                                    nextEvaluation = calendar.getTimeInMillis() + startTimeMilis;
                                } else if ((calendar.getTimeInMillis() + endTimeMilis) > curentDayMilis && (calendar.getTimeInMillis() + endTimeMilis) < nextEvaluation) {
                                    nextEvaluation = calendar.getTimeInMillis() + endTimeMilis;
                                }
                            }
                        } else {
                            Calendar c = Calendar.getInstance();
                            c.setTimeInMillis(calendar.getTimeInMillis());

                            c.set(Calendar.YEAR, currentYear);
                            long milis = c.getTimeInMillis();
                            if (milis < curentDayMilis) {
                                c.set(Calendar.YEAR, currentYear + 1);
                                milis = c.getTimeInMillis();
                            }

                            if (nextEvaluation < 0) {
                                if ((milis + startTimeMilis) > curentDayMilis) {
                                    nextEvaluation = milis + startTimeMilis;
                                } else if ((milis + endTimeMilis) > curentDayMilis) {
                                    nextEvaluation = milis + endTimeMilis;
                                }
                            } else {
                                if ((milis + startTimeMilis) > curentDayMilis && (milis + startTimeMilis) < nextEvaluation) {
                                    nextEvaluation = milis + startTimeMilis;
                                } else if ((milis + endTimeMilis) > curentDayMilis && (milis + endTimeMilis) < nextEvaluation) {
                                    nextEvaluation = milis + endTimeMilis;
                                }
                            }
                        }
                    } else {
                        //weekday
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(curentDayMilis);
                        c.set(Calendar.MILLISECOND, 0);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.HOUR_OF_DAY, 0);

                        while (c.get(Calendar.DAY_OF_WEEK) != statDay.getDay()) {
                            c.add(Calendar.DATE, 1);
                        }
                        if ((c.getTimeInMillis() + endTimeMilis) < curentDayMilis) {
                            c.add(Calendar.DATE, 7);
                        }

                        long milis = c.getTimeInMillis();

                        if (nextEvaluation < 0) {
                            if ((milis + startTimeMilis) > curentDayMilis) {
                                nextEvaluation = milis + startTimeMilis;
                            } else if ((milis + endTimeMilis) > curentDayMilis) {
                                nextEvaluation = milis + endTimeMilis;
                            }
                        } else {
                            if ((milis + startTimeMilis) > curentDayMilis && (milis + startTimeMilis) < nextEvaluation) {
                                nextEvaluation = milis + startTimeMilis;
                            } else if ((milis + endTimeMilis) > curentDayMilis && (milis + endTimeMilis) < nextEvaluation) {
                                nextEvaluation = milis + endTimeMilis;
                            }
                        }
                    }
                }
            }

        }

        log.info("Next evaluation time: " + new Date(nextEvaluation));

        return new Date(nextEvaluation);
    }

    public static boolean evaluateStatDayAvailability(CfgStatDay statDay) {
        // get current date
        Calendar currentCalendar = Calendar.getInstance();
        int currentDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentDayMinutes = currentCalendar.get(Calendar.HOUR_OF_DAY) * 60 + currentCalendar.get(Calendar.MINUTE);

        boolean isStatDayOpen = false;

        if (statDay.getState() == CfgObjectState.CFGEnabled) {
            Integer startTime = statDay.getStartTime();
            Integer endTime = statDay.getEndTime();

            Calendar calendar = statDay.getDate();
            if (statDay.getIsDayOfWeek() != CfgFlag.CFGTrue) {
                if (calendar.get(Calendar.YEAR) > 1970) {
                    if (currentYear == calendar.get(Calendar.YEAR) && currentMonth == calendar.get(Calendar.MONTH) && currentDay == calendar.get(Calendar.DAY_OF_MONTH)) {
                        if (currentDayMinutes > startTime && currentDayMinutes < endTime) {
                            isStatDayOpen = true;
                        }
                    }
                } else {
                    int month = calendar.get(Calendar.MONTH);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);

                    if (day == currentDay && month == currentMonth) {
                        if (currentDayMinutes > startTime && currentDayMinutes < endTime) {
                            isStatDayOpen = true;
                        }
                    }
                }
            } else {
                if (currentDayOfWeek == statDay.getDay()) {
                    if (currentDayMinutes > startTime && currentDayMinutes < endTime) {
                        isStatDayOpen = true;
                    }
                }
            }
        }

        return isStatDayOpen;
    }

    public static boolean isCategoryOpen(CategoryConfig cofiguredCategory, Calendar currentCalendar) {
        
        int currentDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int currentDayOfYear = currentCalendar.get(Calendar.DAY_OF_YEAR);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentDayMinutes = currentCalendar.get(Calendar.HOUR_OF_DAY) * 60 + currentCalendar.get(Calendar.MINUTE);

        boolean isCategoryOpen = false;
        boolean skipDayEvaluation = false;
        boolean skipWeekDayEvaluation = false;

        List<CfgStatDay> openingHours = cofiguredCategory.getOpeningHours();
        for (CfgStatDay statDay : openingHours) {
            if (statDay.getState() == CfgObjectState.CFGEnabled) {
                Integer startTime = statDay.getStartTime();
                Integer endTime = statDay.getEndTime();

                Calendar calendar = statDay.getDate();
                if (statDay.getIsDayOfWeek() != CfgFlag.CFGTrue) {
                    if (calendar.get(Calendar.YEAR) > 1970) {
                        if (currentYear == calendar.get(Calendar.YEAR) && currentMonth == calendar.get(Calendar.MONTH) && currentDay == calendar.get(Calendar.DAY_OF_MONTH)) {
                            skipWeekDayEvaluation = true;
                            skipDayEvaluation = true;

                            if (currentDayMinutes > startTime && currentDayMinutes < endTime) {
                                isCategoryOpen = true;
                            }
                        }
                    }
                }
            }
        }

        if (!skipDayEvaluation) {
            for (CfgStatDay statDay : openingHours) {
                if (statDay.getState() == CfgObjectState.CFGEnabled) {
                    Integer startTime = statDay.getStartTime();
                    Integer endTime = statDay.getEndTime();

                    Calendar calendar = statDay.getDate();
                    if (statDay.getIsDayOfWeek() != CfgFlag.CFGTrue) {
                        if (calendar.get(Calendar.YEAR) == 1970) {
                            Integer dayOfYear = statDay.getDay();
                            if (dayOfYear == currentDayOfYear) {
                                skipWeekDayEvaluation = true;
                                if (currentDayMinutes > startTime && currentDayMinutes < endTime) {
                                    isCategoryOpen = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!skipWeekDayEvaluation) {
            for (CfgStatDay statDay : openingHours) {
                if (statDay.getState() == CfgObjectState.CFGEnabled) {
                    // return only enabled entries
                    Integer startTime = statDay.getStartTime();
                    Integer endTime = statDay.getEndTime();
                    if (statDay.getIsDayOfWeek() == CfgFlag.CFGTrue) {
                        if (currentDayOfWeek == statDay.getDay()) {
                            if (currentDayMinutes > startTime && currentDayMinutes < endTime) {
                                isCategoryOpen = true;
                            }
                        }
                    }
                }
            }
        }

        return isCategoryOpen;
    }

}
