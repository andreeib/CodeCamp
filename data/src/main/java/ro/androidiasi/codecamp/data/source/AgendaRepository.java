package ro.androidiasi.codecamp.data.source;

import android.util.Log;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;

import java.util.ArrayList;
import java.util.List;

import ro.androidiasi.codecamp.data.model.DataCodecamper;
import ro.androidiasi.codecamp.data.model.DataRoom;
import ro.androidiasi.codecamp.data.model.DataSession;
import ro.androidiasi.codecamp.data.model.DataSponsor;
import ro.androidiasi.codecamp.data.model.DataTimeFrame;
import ro.androidiasi.codecamp.data.source.local.AgendaLocalSnappyDataSource;
import ro.androidiasi.codecamp.data.source.local.exception.DataNotFoundException;
import ro.androidiasi.codecamp.data.source.remote.ConnectAPIRemoteDataSource;
import ro.androidiasi.codecamp.data.source.remote.FileRemoteDataSource;

/**
 * Created by andrei on 06/04/16.
 */
@EBean(scope = EBean.Scope.Singleton)
public class AgendaRepository implements IAgendaDataSource<Long> {

    private static final String TAG = "AgendaRepository";
    private DataConference mDataConference;
    @Bean AgendaLocalSnappyDataSource mLocalSnappyDataSource;
    @Bean FileRemoteDataSource mFileRemoteDataSource;
    @Bean ConnectAPIRemoteDataSource mWebViewRemoteDataSource;

    private List<DataRoom> mMemCacheDataRooms;
    private List<DataTimeFrame> mMemCacheTimeFrame;
    private List<DataCodecamper> mMemCacheDataCodecampers;
    private List<DataSession> mMemCacheDataSession;
    private List<DataSponsor> mMemCacheDataSponsors;

    @AfterInject public void afterMembersInject(){
        this.setLatestConference();
    }

    @Background
    @Override public void getRoomsList(boolean pForced, final ILoadCallback<List<DataRoom>> pLoadCallback) {
        if(pForced){
            this.invalidateDataRoomsList();
        }
        this.getRoomsFromRemote(pLoadCallback);
    }

    @Background
    @Override public void getSessionsList(boolean pForced, final ILoadCallback<List<DataSession>> pLoadCallback) {
        if(pForced){
            this.invalidateDataSessions();
        }
        this.getSessionsFromRemote(pLoadCallback);
    }


    @Background
    @Override public void getFavoriteSessionsList(boolean pFroced, final ILoadCallback<List<DataSession>> pLoadCallback) {
        this.getFavoriteSessionsList(pLoadCallback);
    }

    @Background
    @Override public void getTimeFramesList(boolean pForced, final ILoadCallback<List<DataTimeFrame>> pLoadCallback) {
        if(pForced){
            this.inavlidateTimeFrameList();
        }
        this.getTimeFramesFromRemote(pLoadCallback);
    }

    @Background
    @Override public void getCodecampersList(boolean pForced, final ILoadCallback<List<DataCodecamper>> pLoadCallback) {
        if(pForced){
            this.invalidateCodecampersList();
        }
        this.getCodecampersFromRemote(pLoadCallback);
    }
    @Background
    @Override
    public void getSponsorsList(boolean pForced, ILoadCallback<List<DataSponsor>> pLoadCallback) {
        if(pForced){
            this.invalidateDataSponsors();
            this.getSponsorsFromRemote(pLoadCallback);
        } else {
            this.getSponsorsList(pLoadCallback);
        }
    }

    @Background
    @Override public void getRoomsList(final ILoadCallback<List<DataRoom>> pLoadCallback) {
        if(mMemCacheDataRooms != null){
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataRooms);
            return;
        }
        this.mLocalSnappyDataSource.getRoomsList(new ILoadCallback<List<DataRoom>>() {
            @Override public void onSuccess(List<DataRoom> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataRooms = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                mFileRemoteDataSource.getRoomsList(new ILoadCallback<List<DataRoom>>() {
                    @Override public void onSuccess(List<DataRoom> pObject) {
                        if(pObject == null){
                            this.onFailure(new DataNotFoundException());
                            return;
                        }
                        mMemCacheDataRooms = pObject;
                        mLocalSnappyDataSource.storeDataRooms(pObject);
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                        //call this to get the most fresh data :)
                        //don't rely on the local JSON :D
                        this.onFailure(new DataNotFoundException());
                    }

                    @Override public void onFailure(Exception pException) {
                        getRoomsFromRemote(pLoadCallback);
                    }
                });
            }
        });    }

    @Background
    @Override public void getSessionsList(final ILoadCallback<List<DataSession>> pLoadCallback) {
        if(mMemCacheDataSession != null){
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataSession);
            return;
        }
        this.mLocalSnappyDataSource.getSessionsList(new ILoadCallback<List<DataSession>>() {
            @Override public void onSuccess(List<DataSession> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSession = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                mFileRemoteDataSource.getSessionsList(new ILoadCallback<List<DataSession>>() {
                    @Override public void onSuccess(List<DataSession> pObject) {
                        if(pObject == null){
                            this.onFailure(new DataNotFoundException());
                            return;
                        }
                        mMemCacheDataSession = pObject;
                        mLocalSnappyDataSource.storeDataSessions(pObject);
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                        //call this to get the most fresh data :)
                        //don't rely on the local JSON :D
                        this.onFailure(new DataNotFoundException());
                    }

                    @Override public void onFailure(Exception pException) {
                        getSessionsFromRemote(pLoadCallback);
                    }
                });
            }
        });
    }

    @Background
    @Override public void getFavoriteSessionsList(final ILoadCallback<List<DataSession>> pLoadCallback) {
        this.getSessionsList(new ILoadCallback<List<DataSession>>() {
            @Override public void onSuccess(final List<DataSession> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                final List<DataSession> favoriteSessions = new ArrayList<>();
                for (int i = 0; i < pObject.size(); i++) {
                    final int finalI = i;
                    mLocalSnappyDataSource.isSessionFavorite(pObject.get(i).getId(), new ILoadCallback<Boolean>() {
                        @Override public void onSuccess(Boolean pIsFavorite) {
                            if(pIsFavorite) {
                                favoriteSessions.add(pObject.get(finalI));
                            }
                        }

                        @Override public void onFailure(Exception pException) {

                        }
                    });
                }
                onUiThreadCallOnSuccessCallback(pLoadCallback, favoriteSessions);
            }

            @Override public void onFailure(Exception pException) {
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    @Background
    @Override public void getTimeFramesList(final ILoadCallback<List<DataTimeFrame>> pLoadCallback) {
        if(mMemCacheTimeFrame != null){
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheTimeFrame);
            return;
        }
        this.mLocalSnappyDataSource.getTimeFramesList(new ILoadCallback<List<DataTimeFrame>>() {
            @Override public void onSuccess(List<DataTimeFrame> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheTimeFrame = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                mFileRemoteDataSource.getTimeFramesList(new ILoadCallback<List<DataTimeFrame>>() {
                    @Override public void onSuccess(List<DataTimeFrame> pObject) {
                        if(pObject == null){
                            this.onFailure(new DataNotFoundException());
                            return;
                        }
                        mMemCacheTimeFrame = pObject;
                        mLocalSnappyDataSource.storeDataTimeFrames(pObject);
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                        //call this to get the most fresh data :)
                        //don't rely on the local JSON :D
                        this.onFailure(new DataNotFoundException());
                    }

                    @Override public void onFailure(Exception pException) {
                        getTimeFramesFromRemote(pLoadCallback);
                    }
                });
            }
        });
    }

    @Background
    @Override public void getCodecampersList(final ILoadCallback<List<DataCodecamper>> pLoadCallback) {
        if(mMemCacheDataCodecampers != null){
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataCodecampers);
            return;
        }
        this.mLocalSnappyDataSource.getCodecampersList(new ILoadCallback<List<DataCodecamper>>() {
            @Override public void onSuccess(List<DataCodecamper> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataCodecampers = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                mFileRemoteDataSource.getCodecampersList(new ILoadCallback<List<DataCodecamper>>() {
                    @Override public void onSuccess(List<DataCodecamper> pObject) {
                        if(pObject == null){
                            this.onFailure(new DataNotFoundException());
                            return;
                        }
                        mMemCacheDataCodecampers = pObject;
                        mLocalSnappyDataSource.storeDataCodecampers(pObject);
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                        //call this to get the most fresh data :)
                        //don't rely on the local JSON :D
                        this.onFailure(new DataNotFoundException());
                    }

                    @Override public void onFailure(Exception pException) {
                        getCodecampersFromRemote(pLoadCallback);
                    }
                });
            }
        });
    }

    @Background
    @Override public void getSponsorsList(final ILoadCallback<List<DataSponsor>> pLoadCallback) {
        if(mMemCacheDataSponsors != null){
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataSponsors);
        }
        mLocalSnappyDataSource.getSponsorsList(new ILoadCallback<List<DataSponsor>>() {
            @Override public void onSuccess(List<DataSponsor> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSponsors = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                mFileRemoteDataSource.getSponsorsList(new ILoadCallback<List<DataSponsor>>() {
                    @Override public void onSuccess(List<DataSponsor> pObject) {
                        if(pObject == null){
                            this.onFailure(new DataNotFoundException());
                            return;
                        }
                        mMemCacheDataSponsors = pObject;
                        mLocalSnappyDataSource.storeDataSponsors(pObject);
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                        //call this to get the most fresh data :)
                        //don't rely on the local JSON :D
                        this.onFailure(new DataNotFoundException());
                    }

                    @Override public void onFailure(Exception pException) {
                        getSponsorsFromRemote(pLoadCallback);
                    }
                });
            }
        });
    }

    private void getRoomsFromRemote(final ILoadCallback<List<DataRoom>> pLoadCallback) {
        mWebViewRemoteDataSource.getRoomsList(new ILoadCallback<List<DataRoom>>() {
            @Override public void onSuccess(List<DataRoom> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataRooms = pObject;
                mLocalSnappyDataSource.storeDataRooms(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getSessionsFromRemote(final ILoadCallback<List<DataSession>> pLoadCallback) {
        mWebViewRemoteDataSource.getSessionsList(new ILoadCallback<List<DataSession>>() {
            @Override public void onSuccess(List<DataSession> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSession = pObject;
                mLocalSnappyDataSource.storeDataSessions(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getTimeFramesFromRemote(final ILoadCallback<List<DataTimeFrame>> pLoadCallback) {
        mWebViewRemoteDataSource.getTimeFramesList(new ILoadCallback<List<DataTimeFrame>>() {
            @Override public void onSuccess(List<DataTimeFrame> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheTimeFrame = pObject;
                mLocalSnappyDataSource.storeDataTimeFrames(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getCodecampersFromRemote(final ILoadCallback<List<DataCodecamper>> pLoadCallback) {
        mWebViewRemoteDataSource.getCodecampersList(new ILoadCallback<List<DataCodecamper>>() {
            @Override public void onSuccess(List<DataCodecamper> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataCodecampers = pObject;
                mLocalSnappyDataSource.storeDataCodecampers(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getSponsorsFromRemote(final ILoadCallback<List<DataSponsor>> pLoadCallback){
        mWebViewRemoteDataSource.getSponsorsList(new ILoadCallback<List<DataSponsor>>() {
            @Override public void onSuccess(List<DataSponsor> pObject) {
                if(pObject == null){
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSponsors = pObject;
                mLocalSnappyDataSource.storeDataSponsors(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: can't fail more than that :)", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    @Background
    @Override public void getRoom(Long pLong, ILoadCallback<DataRoom> pLoadCallback) {

    }

    @Background
    @Override public void getSession(Long pLong, ILoadCallback<DataSession> pLoadCallback) {

    }

    @Background
    @Override public void getTimeFrame(Long pLong, ILoadCallback<DataTimeFrame> pLoadCallback) {

    }

    @Background
    @Override public void getCodecamper(Long pLong, ILoadCallback<DataCodecamper> pLoadCallback) {

    }

    @Background
    @Override public void isSessionFavorite(Long pLong, final ILoadCallback<Boolean> pLoadCallback) {
        mLocalSnappyDataSource.isSessionFavorite(pLong, new ILoadCallback<Boolean>() {
            @Override public void onSuccess(Boolean pObject) {
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    @Background
    @Override public void setSessionFavorite(Long pLong, boolean pFavorite, final ILoadCallback<Boolean> pLoadCallback) {
        this.mLocalSnappyDataSource.setSessionFavorite(pLong, pFavorite, new ILoadCallback<Boolean>() {
            @Override public void onSuccess(Boolean pObject) {
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    public void setConference(DataConference pConference) {
        this.mDataConference = pConference;
        this.mLocalSnappyDataSource.setConference(pConference);
        this.mFileRemoteDataSource.setConference(pConference);
        this.mWebViewRemoteDataSource.setConference(pConference);
    }

    @Override public DataConference getConference() {
        return mDataConference;
    }

    @UiThread public <Model> void onUiThreadCallOnSuccessCallback(ILoadCallback<Model> pLoadCallback, Model pModel) {
        pLoadCallback.onSuccess(pModel);
    }

    @UiThread public <E extends Exception> void onUiThreadCallOnFailureCallback(ILoadCallback pLoadCallback, E pException) {
        pLoadCallback.onFailure(pException);
    }

    public void setLatestConference(){
        this.setConference(DataConference.getLatestEvent());
    }

    private void invalidateDataRoomsList() {
        this.mMemCacheDataRooms = null;
        this.mLocalSnappyDataSource.invalidateDataRooms();
    }

    private void inavlidateTimeFrameList(){
        this.mMemCacheTimeFrame = null;
        this.mLocalSnappyDataSource.invalidateDataTimeFrames();
    }

    private void invalidateCodecampersList(){
        this.mMemCacheDataCodecampers = null;
        this.mLocalSnappyDataSource.invalidateDataCodecampers();
    }

    private void invalidateDataSessions(){
        this.mMemCacheDataSession = null;
        this.mLocalSnappyDataSource.invalidateDataSessions();
    }

    private void invalidateDataSponsors(){
        this.mMemCacheDataSponsors = null;
        this.mLocalSnappyDataSource.invalidateDataSponsors();
    }

    @Override public void invalidate(){
        this.mMemCacheDataRooms = null;
        this.mMemCacheTimeFrame = null;
        this.mMemCacheDataCodecampers = null;
        this.mMemCacheDataSession = null;
        this.mMemCacheDataSponsors = null;
        this.mLocalSnappyDataSource.invalidate();
    }
}
