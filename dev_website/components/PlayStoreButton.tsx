import React from 'react'
import clsx from 'clsx'
import Image from 'next/image'

import { ctaDetails } from '@/data/cta'

const PlayStoreButton = ({ dark }: { dark?: boolean }) => {
    return (
        <a href={ctaDetails.googlePlayUrl}>
            <img
                src="/Google_Play_Store_badge_EN.svg"
                alt="Get it on Google Play"
                className="h-16"
            />
        </a>
    )
}

export default PlayStoreButton