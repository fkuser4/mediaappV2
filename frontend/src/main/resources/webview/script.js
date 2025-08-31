const initialMessage = document.getElementById('initial-message');
const previewWrapper = document.getElementById('post-preview-wrapper');
const linkPreviewTemplate = document.getElementById('link-preview-template');

let currentPlatform = 'twitter';
let currentData = null;
let currentInstagramSlide = 0;


function showInitialState() {
    if (previewWrapper) previewWrapper.style.display = 'none';
    if (initialMessage) initialMessage.style.display = 'flex';
}


function loadPreviewFromJava(data) {
    if (initialMessage) initialMessage.style.display = 'none';
    if (previewWrapper) previewWrapper.style.display = 'block';

    currentData = data;

    updatePlatformTabsVisibility(currentData.platforms);

    updatePlatformPreview();
}

function updatePlatformTabsVisibility(availablePlatforms) {
    if (!availablePlatforms || availablePlatforms.length === 0) {
        availablePlatforms = ['twitter', 'instagram', 'facebook', 'youtube', 'tiktok'];
    }
    const allowedPlatforms = availablePlatforms.map(p => p.toLowerCase());

    const indexOfX = allowedPlatforms.indexOf('x');
    if (indexOfX !== -1) {
        allowedPlatforms[indexOfX] = 'twitter';
    }

    const allTabs = document.querySelectorAll('.tab-button');
    allTabs.forEach(tab => {
        const platform = tab.dataset.platform;
        if (allowedPlatforms.includes(platform)) {
            tab.style.display = 'flex';
        } else {
            tab.style.display = 'none';
        }
    });

    const activeTab = document.querySelector('.tab-button.active');
    if (activeTab && activeTab.style.display === 'none') {
        const firstVisibleTab = document.querySelector('.tab-button:not([style*="display: none"])');
        if (firstVisibleTab) {
            console.log("Active tab was hidden, switching to first available:", firstVisibleTab.dataset.platform);
            firstVisibleTab.click();
        }
    }
}

document.querySelectorAll('.tab-button').forEach(button => {
    button.addEventListener('click', function() {
        document.querySelectorAll('.tab-button').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.platform-preview').forEach(preview => preview.classList.remove('active'));

        this.classList.add('active');
        currentPlatform = this.dataset.platform;
        const newPreview = document.getElementById(currentPlatform + '-preview');
        if (newPreview) {
            newPreview.classList.add('active');
        }

        if (currentData) {
            updatePlatformPreview();
        }
        handleVideoPlayback();
    });
});

function updatePlatformPreview() {
    if (!currentData) return;
    const { content, mediaType, user, engagement, mediaUris } = currentData;
    updateUserData(user);
    updateEngagementData(engagement);
    updateContent(content);
    updateMedia(mediaUris, mediaType);
    setTimeout(handleVideoPlayback, 100);
}

function updateUserData(user) {
    if (!user) return;
    const { username, displayName, avatarUrl, verified } = user;

    const avatarElements = ['twitterAvatar', 'instagramAvatar', 'facebookAvatar', 'youtubeAvatar', 'tiktokAvatar'];
    avatarElements.forEach(id => {
        const element = document.getElementById(id);
        if (element && avatarUrl) {
            element.style.backgroundImage = `url(${avatarUrl})`;
        }
    });

    const elementsToUpdate = {
        'twitterDisplayName': displayName, 'twitterUsername': '@' + username,
        'instagramUsername': username, 'instagramCaptionUser': username,
        'facebookDisplayName': displayName, 'youtubeChannelName': displayName,
        'tiktokUsername': username
    };

    for (const [id, text] of Object.entries(elementsToUpdate)) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = text;
        }
    }

    document.querySelectorAll('.verified-badge').forEach(badge => {
        badge.style.display = verified ? 'inline-block' : 'none';
    });
}

function updateEngagementData(engagement) {
    if (!engagement) return;
    const { likes, comments, shares, views } = engagement;

    const elementsToUpdate = {
        'twitterLikes': likes, 'twitterComments': comments, 'twitterShares': shares,
        'instagramLikes': likes + ' likes', 'instagramCommentsLink': `View all ${comments} comments`,
        'facebookLikes': likes, 'facebookCommentsShares': `${comments} comments · ${shares} shares`,
        'youtubeViews': `${views} views • 2 hours ago`,
        'tiktokLikes': formatCount(parseInt(String(likes || '0').replace(/,/g, ''))),
        'tiktokComments': comments, 'tiktokShares': shares
    };

    for (const [id, text] of Object.entries(elementsToUpdate)) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = text;
        }
    }
}

function updateContent(content) {
    const textContent = content || '';
    const elementsToUpdate = {
        'twitterText': textContent, 'instagramCaption': ' ' + textContent,
        'facebookContent': textContent, 'youtubeTitle': textContent,
        'tiktokCaption': textContent
    };
    for (const [id, text] of Object.entries(elementsToUpdate)) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = text;
        }
    }
}

function createLinkPreview(container) {
    if (!container || !linkPreviewTemplate) return;
    container.innerHTML = '';
    const previewClone = linkPreviewTemplate.firstElementChild.cloneNode(true);
    container.appendChild(previewClone);
}

function updateMedia(mediaUris, mediaType) {
    const mediaArray = Array.isArray(mediaUris) ? mediaUris : [];
    const platformUpdater = {
        'twitter': updateTwitterMedia, 'instagram': updateInstagramMedia,
        'facebook': updateFacebookMedia, 'youtube': updateYouTubeMedia,
        'tiktok': updateTikTokMedia
    };
    const updater = platformUpdater[currentPlatform];
    if (updater) {
        updater(mediaArray, mediaType);
    }
}

function updateTwitterMedia(mediaArray, mediaType) {
    const container = document.getElementById('twitterMedia');
    if (!container) return;
    container.innerHTML = '';

    if (mediaType === 'LINK') {
        createLinkPreview(container);
        return;
    }
    if (!mediaArray || mediaArray.length === 0) return;

    if (mediaArray.length === 1) {
        container.innerHTML = mediaType === 'IMAGE'
            ? `<img src="${mediaArray[0]}" alt="Tweet image">`
            : `<video src="${mediaArray[0]}" controls autoplay muted loop playsinline></video>`;
    } else {
        const gridClass = ['', '', 'two', 'three', 'four'][mediaArray.length] || 'four';
        const items = mediaArray.slice(0, 4).map(url => `<div class="x-grid-item"><img src="${url}" alt="Tweet image"></div>`).join('');
        container.innerHTML = `<div class="x-media-grid ${gridClass}">${items}</div>`;
    }
}

function updateInstagramMedia(mediaArray, mediaType) {
    const container = document.getElementById('instagramMedia');
    if (!container) return;
    container.innerHTML = '';

    if (mediaType === 'LINK' || !mediaArray || mediaArray.length === 0) return;

    if (mediaArray.length === 1) {
        container.innerHTML = mediaType === 'IMAGE'
            ? `<img src="${mediaArray[0]}" alt="Instagram post">`
            : `<video src="${mediaArray[0]}" controls autoplay muted loop playsinline></video>`;
    } else {
        const slides = mediaArray.map(url => `<div class="ig-carousel-slide">${mediaType === 'IMAGE' ? `<img src="${url}" alt="Instagram post">` : `<video src="${url}" muted loop playsinline></video>`}</div>`).join('');
        const dots = mediaArray.map((_, i) => `<div class="ig-dot ${i === 0 ? 'active' : ''}" onclick="goToInstagramSlide(${i})"></div>`).join('');
        container.innerHTML = `<div class="ig-carousel"><div class="ig-carousel-container">${slides}</div><button class="ig-carousel-nav prev" onclick="changeInstagramSlide(-1)">‹</button><button class="ig-carousel-nav next" onclick="changeInstagramSlide(1)">›</button><div class="ig-dots">${dots}</div></div>`;
        currentInstagramSlide = 0;
    }
}

function updateFacebookMedia(mediaArray, mediaType) {
    const container = document.getElementById('facebookMedia');
    if (!container) return;
    container.innerHTML = '';

    if (mediaType === 'LINK') {
        createLinkPreview(container);
        return;
    }
    if (!mediaArray || mediaArray.length === 0) return;

    const firstMediaUrl = mediaArray[0];
    const firstMedia = mediaType === 'IMAGE'
        ? `<img src="${firstMediaUrl}" alt="Facebook post">`
        : `<video src="${firstMediaUrl}" controls autoplay muted loop playsinline></video>`;

    if (mediaArray.length === 1) {
        container.innerHTML = firstMedia;
    } else {
        container.innerHTML = `
            <div style="position: relative;">
                ${firstMedia}
                <div class="fb-more-overlay">+${mediaArray.length - 1}</div>
            </div>`;
    }
}

function updateYouTubeMedia(mediaArray, mediaType) {
    const container = document.getElementById('youtubeMedia');
    if (!container) return;
    container.innerHTML = '';

    if (mediaType === 'LINK' || !mediaArray || mediaArray.length === 0) return;

    const mediaUrl = mediaArray[0];
    if (mediaType === 'IMAGE') {
        container.innerHTML = `<img src="${mediaUrl}" alt="YouTube thumbnail">`;
    } else {
        container.innerHTML = `<video src="${mediaUrl}" controls style="width:100%; height:100%;"></video>`;
    }
}

function updateTikTokMedia(mediaArray, mediaType) {
    const container = document.getElementById('tiktokMedia');
    if (!container) return;
    container.innerHTML = '';

    if (mediaType === 'LINK' || !mediaArray || mediaArray.length === 0) return;

    const mediaUrl = mediaArray[0];
    if (mediaType === 'IMAGE') {
        container.innerHTML = `<img src="${mediaUrl}" alt="TikTok content">`;
    } else {
        container.innerHTML = `<video src="${mediaUrl}" autoplay muted loop playsinline></video>`;
    }
}

function changeInstagramSlide(direction) {
    const container = document.querySelector('.ig-carousel-container');
    if (!container) return;
    const totalSlides = container.children.length;
    if (totalSlides <= 1) return;
    currentInstagramSlide = (currentInstagramSlide + direction + totalSlides) % totalSlides;
    goToInstagramSlide(currentInstagramSlide);
}

function goToInstagramSlide(index) {
    const container = document.querySelector('.ig-carousel-container');
    const dots = document.querySelectorAll('.ig-dot');
    if (!container || !dots) return;
    currentInstagramSlide = index;
    container.style.transform = `translateX(-${index * 100}%)`;
    dots.forEach((dot, i) => dot.classList.toggle('active', i === index));
}
window.changeInstagramSlide = changeInstagramSlide;
window.goToInstagramSlide = goToInstagramSlide;

function formatCount(num) {
    if (isNaN(num)) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(1).replace(/\.0$/, '') + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1).replace(/\.0$/, '') + 'K';
    return num.toString();
}

function handleVideoPlayback() {
    document.querySelectorAll('video').forEach(video => {
        video.pause();
        if (video.closest('.platform-preview.active')) {
            video.play().catch(e => console.warn("Autoplay was prevented by the browser:", e));
        }
    });
}